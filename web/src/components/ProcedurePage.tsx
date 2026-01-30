import React, {useState, useEffect, useCallback} from 'react';
import {
  Table,
  Button,
  Space,
  Typography,
  Form,
  Input,
  Select,
  Row,
  Col,
  Card,
  DatePicker,
  Tag,
  Spin,
  Alert,
  Modal,
  message
} from 'antd';
import {createTimeslot} from '../services/api.ts';
import type {ColumnsType} from 'antd/es/table';
import {queryProcedures} from '../services/api.ts';
import type {ProcedureQueryDTO} from '../services/model.ts';
import {SearchOutlined, FilterOutlined} from '@ant-design/icons';

// 添加任务行背景色样式
const styles = `
  .task-row-even {
    background-color: #e6f7ff;
  }
  .task-row-odd {
    background-color: #ffffff;
  }
`;

const {Text} = Typography;
const {Option} = Select;
const {RangePicker} = DatePicker;

const ProcedurePage: React.FC = () => {
    // 状态管理
    const [form] = Form.useForm();
    const [procedures, setProcedures] = useState<ProcedureQueryDTO[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [pageSize, setPageSize] = useState<number>(20);
    const [total, setTotal] = useState<number>(0);
    const [filterVisible, setFilterVisible] = useState<boolean>(false);
    
    // 切分工序对话框状态
    const [splitModalVisible, setSplitModalVisible] = useState<boolean>(false);
    // 切分工序表单
    const [splitForm] = Form.useForm();
    // 当前选中的工序
    const [selectedProcedure, setSelectedProcedure] = useState<ProcedureQueryDTO | null>(null);
    // 对话框加载状态
    const [splitLoading, setSplitLoading] = useState<boolean>(false);
    // 输入框值状态，用于控制互斥禁用
    const [minWorkTime, setMinWorkTime] = useState<number | undefined>(undefined);
    const [splitDays, setSplitDays] = useState<number | undefined>(undefined);
    // 任务号到索引的映射，用于确保不同任务号交替显示背景色
    const [taskNoIndexMap, setTaskNoIndexMap] = useState<Record<string, number>>({});

    // 工序状态选项
    const procedureStatusOptions = [
        {label: '待执行', value: '待执行', color: 'blue'},
        {label: '执行中', value: '执行中', color: 'green'},
        {label: '执行完成', value: '执行完成', color: 'orange'},
        {label: '待质检', value: '待质检', color: 'red'},
        {label: '初始导入', value: '初始导入', color: 'purple'},
    ];

    // 任务状态选项
    const taskStatusOptions = [
        {label: '待生产', value: '待生产', color: 'blue'},
        {label: '生产中', value: '生产中', color: 'green'},
        {label: '生产完成', value: '生产完成', color: 'orange'},
        {label: '待质检', value: '待质检', color: 'purple'},
        {label: '质检中', value: '质检中', color: 'cyan'},
        {label: '质检完成', value: '质检完成', color: 'success'},
        {label: '已锁定', value: '已锁定', color: 'gray'},
        {label: '已删除', value: '已删除', color: 'default'},
        {label: '已暂停', value: '已暂停', color: 'red'},
    ];

    // 获取工序数据
    const loadProcedures = useCallback(async (page: number = 1, size: number = 20) => {
        setLoading(true);
        setError(null);
        try {
            const values = form.getFieldsValue();
            // 处理日期范围
            let startDate: string | undefined;
            let endDate: string | undefined;
            if (values.dateRange && values.dateRange.length === 2) {
                startDate = values.dateRange[0].format('YYYY-MM-DD');
                endDate = values.dateRange[1].format('YYYY-MM-DD');
            }
            const response = await queryProcedures({
                orderName: values.orderName,
                taskNo: values.taskNo,
                contractNum: values.contractNum,
                productCode: values.productCode,
                statusList: values.procedureStatus ? [values.procedureStatus] : undefined,
                startDate,
                endDate,
                pageNum: page,
                pageSize: size
            });
            if (response.code === 200) {
                const procedureList = response.data?.content || [];
                // 生成任务号到索引的映射，确保不同任务号的索引递增
                const taskMap: Record<string, number> = {};
                let taskIndex = 0;
                procedureList.forEach(procedure => {
                    if (!taskMap[procedure.taskNo]) {
                        taskMap[procedure.taskNo] = taskIndex++;
                    }
                });
                setTaskNoIndexMap(taskMap);
                setProcedures(procedureList);
                setTotal(response.data?.totalElements || 0);
                setCurrentPage(page);
                setPageSize(size);
            } else {
                setError(response.msg || '获取数据失败');
                setProcedures([]);
                setTotal(0);
                setTaskNoIndexMap({});
            }
        } catch {
            setError('网络错误，请稍后重试');
            setProcedures([]);
            setTotal(0);
            setTaskNoIndexMap({});
        } finally {
            setLoading(false);
        }
    }, [form]);

    // 初始加载
    useEffect(() => {
        loadProcedures();
    }, [loadProcedures]);

    // 处理搜索
    const handleSearch = () => {
        loadProcedures(1, pageSize);
    };

    // 处理重置
    const handleReset = () => {
        form.resetFields();
        loadProcedures(1, pageSize);
    };

    // 处理分页
    const handlePaginationChange = (page: number, size: number) => {
        loadProcedures(page, size);
    };
    
    // 打开切分工序对话框
    const handleOpenSplitModal = (procedure: ProcedureQueryDTO) => {
        setSelectedProcedure(procedure);
        splitForm.resetFields();
        setMinWorkTime(undefined);
        setSplitDays(undefined);
        setSplitModalVisible(true);
    };
    
    // 关闭切分工序对话框
    const handleCloseSplitModal = () => {
        setSplitModalVisible(false);
        setSelectedProcedure(null);
        splitForm.resetFields();
        setMinWorkTime(undefined);
        setSplitDays(undefined);
    };
    
    // 提交切分工序
    const handleSplitProcedure = async () => {
        try {
            const values = await splitForm.validateFields();
            
            if (!selectedProcedure) {
                message.error('请选择要切分的工序');
                return;
            }
            
            setSplitLoading(true);
            
            // 调用createTimeslot接口
            await createTimeslot(
                selectedProcedure.procedureId,
                values.minWorkTime || 0,
                values.splitDays || 0
            );
            
            message.success('提交成功');
            setSplitModalVisible(false);
            setSelectedProcedure(null);
            splitForm.resetFields();
            
            // 重新加载数据
            loadProcedures(currentPage, pageSize);
        } catch (error) {
            message.error(`分配失败: ${error instanceof Error ? error.message : '未知错误'}`);
        } finally {
            setSplitLoading(false);
        }
    };

    // 获取状态标签
    const getStatusTag = (status: string, options: Array<{ label: string; value: string; color: string }>) => {
        const option = options.find(opt => opt.value === status);
        if (option) {
            return <Tag color={option.color}>{option.label}</Tag>;
        }
        return <Tag>{status}</Tag>;
    };

    // 表格列定义
    const columns: ColumnsType<ProcedureQueryDTO> = [
        {
            title: '操作',
            key: 'action',
            width: 90,
            fixed: 'left' as const,
            render: (_, record) => {
                const buttonText = record.procedureType === 'ZP02' ? '外协安排' : '工序拆分';
                const isDisabled = record.procedureStatus === '执行完成';
                return (
                    <div style={{ textAlign: 'center' }}>
                        <Button 
                            size="small" 
                            type="primary" 
                            onClick={() => handleOpenSplitModal(record)}
                            disabled={isDisabled}
                        >
                            {buttonText}
                        </Button>
                    </div>
                );
            },
        },
        {
            title: '订单信息',
            dataIndex: 'orderNo',
            key: 'orderInfo',
            minWidth: 140,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 13, fontWeight: 'bold'}}>任务:{record.taskNo}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 2}}>订单: {record.orderNo}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 1}}>合同: {record.contractNum}</div>
                </div>
            ),
        },
        {
            title: '产品名称',
            dataIndex: 'productName',
            key: 'productInfo',
            minWidth: 180,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 13, fontWeight: 'bold'}}>{record.productName}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 2}}>产品代码: {record.productCode}</div>
                </div>
            ),

        },
        {
            title: '工序信息',
            dataIndex: 'procedureName',
            key: 'procedureInfo',
            minWidth: 160,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 13, fontWeight: 'bold'}}>名称: {record.procedureName}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 2}}>工序: {record.procedureNo}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 1}}>工作中心: {record.workCenterName}</div>
                </div>
            ),
        },
        {
            title: '状态',
            dataIndex: 'procedureStatus',
            key: 'status',
            minWidth: 140,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11, marginBottom: 2}}>工序状态: {getStatusTag(record.procedureStatus, procedureStatusOptions)}</div>
                    <div style={{fontSize: 11}}>任务状态: {getStatusTag(record.taskStatus, taskStatusOptions)}</div>
                </div>
            ),
        },
        {
            title: '时间信息',
            dataIndex: 'planStartDate',
            key: 'timeInfo',
            minWidth: 200,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11, marginBottom: 2}}>计划: {record.planStartDate} 至 {record.planEndDate}</div>
                    <div style={{fontSize: 11}}>实际: {record.startTime || '-'}</div>
                </div>
            ),
        },
        {
            title: '工时信息',
            dataIndex: 'humanMinutes',
            key: 'workTime',
            minWidth: 100,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11, marginBottom: 1}}>人工: {record.humanMinutes} 分钟</div>
                    <div style={{fontSize: 11}}>机器: {record.machineMinutes} 分钟</div>
                    <div style={{fontSize: 11}}>预计天数: {record.timeslotDays} 天</div>
                </div>
            ),
        },

    ];

    return (
        <div style={{minHeight: '100vh', backgroundColor: '#f0f2f5'}}>
            <style>{styles}</style>
            {/* 主要内容 */}
            <div style={{padding: 2}}>
                {/* 查询条件 */}
                <Card
                    title={
                        <Space>
                            <FilterOutlined/>
                            <Text>查询条件</Text>
                        </Space>
                    }
                    style={{marginBottom: 6, borderRadius: 6, boxShadow: '0 1px 3px rgba(0,0,0,0.1)', padding: '8px 6px'}}
                    extra={
                        <Button
                            type="link"
                            size="small"
                            onClick={() => setFilterVisible(!filterVisible)}
                        >
                            {filterVisible ? '收起筛选' : '展开筛选'}
                        </Button>
                    }
                >
                    <Form
                        form={form}
                        layout="horizontal"
                        labelCol={{ span: 6 }}
                        wrapperCol={{ span: 18 }}
                        size="small"
                        style={{ marginBottom: 0 }}
                    >
                        <Row gutter={[8, 8]}>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="orderNo" label="订单编号" style={{ marginBottom: 4 }}>
                                    <Input placeholder="请输入订单编号" size="small" style={{ height: 24 }}/>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="taskNo" label="任务编号" style={{ marginBottom: 4 }}>
                                    <Input placeholder="请输入任务编号" size="small" style={{ height: 24 }}/>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="procedureStatus" label="工序状态" style={{ marginBottom: 4 }}>
                                    <Select placeholder="请选择工序状态" allowClear size="small" style={{ height: 24 }}>
                                        {procedureStatusOptions.map(option => (
                                            <Option key={option.value} value={option.value}>{option.label}</Option>
                                        ))}
                                    </Select>
                                </Form.Item>
                            </Col>

                            {filterVisible && (
                                <>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="contractNum" label="合同编号" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入合同编号" size="small" style={{ height: 24 }}/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="orderName" label="订单名称" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入订单名称" size="small" style={{ height: 24 }}/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="productCode" label="产品编码" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入产品编码" size="small" style={{ height: 24 }}/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="productName" label="产品名称" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入产品名称" size="small" style={{ height: 24 }}/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="dateRange" label="日期范围" style={{ marginBottom: 4 }}>
                                            <RangePicker style={{width: '100%', height: 24 }} size="small"/>
                                        </Form.Item>
                                    </Col>
                                </>
                            )}

                            <Col xs={24} sm={24} md={8} lg={6}>
                                <Form.Item label="" style={{ marginBottom: 4 }}>
                                    <Space size="small" style={{ width: '100%', justifyContent: 'flex-start' }}>
                                        <Button
                                            type="primary"
                                            size="small"
                                            icon={<SearchOutlined/>}
                                            onClick={handleSearch}
                                            loading={loading}
                                            style={{ height: 24, padding: '0 12px' }}
                                        >
                                            搜索
                                        </Button>
                                        <Button size="small" onClick={handleReset} style={{ height: 24, padding: '0 12px' }}>重置</Button>
                                    </Space>
                                </Form.Item>
                            </Col>
                        </Row>
                    </Form>
                </Card>

                {/* 错误提示 */}
                {error && (
                    <Alert
                        message="错误提示"
                        description={error}
                        type="error"
                        showIcon
                        style={{marginBottom: 16}}
                        action={
                            <Button size="small" onClick={() => loadProcedures(currentPage, pageSize)}>
                                重试
                            </Button>
                        }
                    />
                )}

                {/* 工序表格 */}
                <Card style={{borderRadius: 6, border: '1px solid #d9d9d9', boxShadow: '0 1px 3px rgba(0,0,0,0.1)'}}>
                    <Spin spinning={loading} tip="加载中...">
                        <Table
                            columns={columns}
                            dataSource={procedures}
                            rowKey={(record) => `${record.taskNo}_${record.procedureNo}`}
                            rowClassName={(record) => {
                                // 使用任务号到索引的映射来生成背景色类名，确保不同任务号交替显示
                                const taskIndex = taskNoIndexMap[record.taskNo] || 0;
                                return taskIndex % 2 === 0 ? 'task-row-even' : 'task-row-odd';
                            }}
                            pagination={{
                                current: currentPage,
                                pageSize: pageSize,
                                total: total,
                                onChange: handlePaginationChange,
                                showSizeChanger: true,
                                pageSizeOptions: ['10', '20', '50', '100'],
                                showTotal: (total) => `共 ${total} 条记录`,
                                showQuickJumper: true,
                                size: 'small'
                            }}
                            scroll={{x: 900}}
                            bordered
                            size="small"
                            locale={{
                                emptyText: (
                                    <div style={{textAlign: 'center', padding: 32}}>
                                        <div style={{fontSize: 32, color: '#ccc', marginBottom: 12}}>📋</div>
                                        <Text style={{fontSize: 14, color: '#999'}}>暂无工序数据</Text>
                                    </div>
                                )
                            }}
                        />
                    </Spin>
                </Card>
                
                {/* 切分工序对话框 */}
                <Modal
                    title={selectedProcedure?.procedureType === 'ZP02' ? '外协安排' : '工序拆分'}
                    open={splitModalVisible}
                    onCancel={handleCloseSplitModal}
                    footer={[
                        <Button key="cancel" onClick={handleCloseSplitModal}>
                            取消
                        </Button>,
                        <Button
                            key="submit"
                            type="primary"
                            loading={splitLoading}
                            onClick={handleSplitProcedure}
                        >
                            确认
                        </Button>,
                    ]}
                >
                    <Form
                        form={splitForm}
                        layout="vertical"
                        style={{ maxWidth: 600 }}
                    >
                        <Form.Item
                            hidden={true}
                            name="minWorkTime"
                            label="最小工时（分钟）"
                            rules={[
                                {
                                    validator: (_, value, callback) => {
                                        if (value && splitDays) {
                                            callback('最小工时和分拆天数只能填写一个');
                                        } else {
                                            callback();
                                        }
                                    },
                                },
                            ]}
                        >
                            <Input 
                                type="number" 
                                placeholder="请输入最小工时" 
                                min={1} 
                                step={1}
                                disabled={!!splitDays}
                                onChange={(e) => setMinWorkTime(e.target.value ? Number(e.target.value) : undefined)}
                            />
                        </Form.Item>
                        <Form.Item
                            name="splitDays"
                            label="预计天数"
                            rules={[
                                {
                                    validator: (_, value, callback) => {
                                        if (value && minWorkTime) {
                                            callback('最小工时和分拆天数只能填写一个');
                                        } else {
                                            callback();
                                        }
                                    },
                                },
                            ]}
                        >
                            <Input 
                                type="number" 
                                placeholder="请输入天数"
                                min={1} 
                                step={1}
                                disabled={!!minWorkTime}
                                onChange={(e) => setSplitDays(e.target.value ? Number(e.target.value) : undefined)}
                            />
                        </Form.Item>
                    </Form>
                </Modal>
            </div>
        </div>
    );
};

export default ProcedurePage;
