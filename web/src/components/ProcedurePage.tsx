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
  Modal,
  Tag,
  Spin,
  Alert,
  Tooltip
} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import {queryProcedures} from '../services/api.ts';
import type {ProcedureQueryDTO} from '../services/model.ts';
import {SearchOutlined, FilterOutlined, CalendarOutlined, ClockCircleOutlined} from '@ant-design/icons';

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
    const [selectedProcedure, setSelectedProcedure] = useState<ProcedureQueryDTO | null>(null);

    // 工序状态选项
    const procedureStatusOptions = [
        {label: '待执行', value: '待执行', color: 'blue'},
        {label: '执行中', value: '执行中', color: 'green'},
        {label: '已完成', value: '已完成', color: 'orange'},
        {label: '已暂停', value: '已暂停', color: 'red'},
        {label: '初始导入', value: '初始导入', color: 'purple'},
    ];

    // 任务状态选项
    const taskStatusOptions = [
        {label: '待生产', value: '待生产', color: 'blue'},
        {label: '生产中', value: '生产中', color: 'green'},
        {label: '生产完成', value: '生产完成', color: 'orange'},
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
                setProcedures(response.data?.content || []);
                setTotal(response.data?.totalElements || 0);
                setCurrentPage(page);
                setPageSize(size);
            } else {
                setError(response.msg || '获取数据失败');
                setProcedures([]);
                setTotal(0);
            }
        } catch {
            setError('网络错误，请稍后重试');
            setProcedures([]);
            setTotal(0);
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
            title: '订单信息',
            dataIndex: 'orderNo',
            key: 'orderInfo',
            minWidth: 200,
            render: (_, record) => (
                <div>
                    <div style={{fontWeight: 'bold', marginBottom: 4}}>任务:{record.taskNo}</div>
                    <div style={{fontSize: 12, color: '#666'}}>订单: {record.orderNo}</div>
                    <div style={{fontSize: 12, color: '#666'}}>合同: {record.contractNum}</div>
                </div>
            ),
        },
        {
            title: '产品名称',
            dataIndex: 'productName',
            key: 'productInfo',
            minWidth: 300,
            render: (_, record) => (
                <div>
                    <div style={{ fontWeight: 'bold', marginBottom: 4 }}>{record.productName}</div>
                    <div style={{ fontSize: 12, color: '#666' }}>产品代码: {record.productCode}</div>
                </div>
            ),

        },
        {
            title: '工序信息',
            dataIndex: 'procedureName',
            key: 'procedureInfo',
            minWidth: 200,
            render: (_, record) => (
                <div>
                    <div style={{fontWeight: 'bold', marginBottom: 4}}>名称: {record.procedureName}</div>
                    <div style={{fontSize: 12, color: '#666'}}>工序: {record.procedureNo}</div>
                    <div style={{fontSize: 12, color: '#666'}}>工作中心: {record.workCenterName}</div>
                </div>
            ),
        },
        {
            title: '状态',
            dataIndex: 'procedureStatus',
            key: 'status',
            minWidth: 180,
            render: (_, record) => (
                <div>
                    <div
                        style={{marginBottom: 4}}>工序状态: {getStatusTag(record.procedureStatus, procedureStatusOptions)}</div>
                    <div>任务状态: {getStatusTag(record.taskStatus, taskStatusOptions)}</div>
                </div>
            ),
        },
        {
            title: '时间信息',
            dataIndex: 'planStartDate',
            key: 'timeInfo',
            minWidth: 300,
            render: (_, record) => (
                <div>
                    <div style={{marginBottom: 8, display: 'flex', alignItems: 'center'}}>
                        <CalendarOutlined style={{marginRight: 8, fontSize: 12}}/>
                        <Text style={{fontSize: 12}}>计划: {record.planStartDate} 至 {record.planEndDate}</Text>
                    </div>
                    <div style={{display: 'flex', alignItems: 'center'}}>
                        <ClockCircleOutlined style={{marginRight: 8, fontSize: 12}}/>
                        <Text style={{fontSize: 12}}>实际: {record.startTime || '-'}</Text>
                    </div>
                </div>
            ),
        },
        {
            title: '工时信息',
            dataIndex: 'humanMinutes',
            key: 'workTime',
            minWidth: 140,
            render: (_, record) => (
                <div>
                    <div style={{marginBottom: 4}}>人工: {record.humanMinutes} 分钟</div>
                    <div>机器: {record.machineMinutes} 分钟</div>
                </div>
            ),
        },
        {
            title: '操作',
            key: 'action',
            width: 100,
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="查看详情">
                        <Button
                            size="small"
                            type="link"
                            onClick={() => setSelectedProcedure(record)}
                        >
                            详情
                        </Button>
                    </Tooltip>
                </Space>
            ),
        },
    ];

    return (
        <div style={{minHeight: '100vh', backgroundColor: '#f0f2f5'}}>

            {/* 主要内容 */}
            <div style={{padding: 32}}>
                {/* 查询条件 */}
                <Card
                    title={
                        <Space>
                            <FilterOutlined/>
                            <Text>查询条件</Text>
                        </Space>
                    }
                    style={{marginBottom: 24, borderRadius: 8}}
                    extra={
                        <Button
                            type="link"
                            onClick={() => setFilterVisible(!filterVisible)}
                        >
                            {filterVisible ? '收起筛选' : '展开筛选'}
                        </Button>
                    }
                >
                    <Form
                        form={form}
                        layout="vertical"
                        size="middle"
                    >
                        <Row gutter={[16, 16]}>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="orderNo" label="订单编号">
                                    <Input placeholder="请输入订单编号"/>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="taskNo" label="任务编号">
                                    <Input placeholder="请输入任务编号"/>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="procedureStatus" label="工序状态">
                                    <Select placeholder="请选择工序状态" allowClear>
                                        {procedureStatusOptions.map(option => (
                                            <Option key={option.value} value={option.value}>{option.label}</Option>
                                        ))}
                                    </Select>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="contractNum" label="合同编号">
                                    <Input placeholder="请输入合同编号"/>
                                </Form.Item>
                            </Col>

                            {filterVisible && (
                                <>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="orderName" label="订单名称">
                                            <Input placeholder="请输入订单名称"/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="productCode" label="产品编码">
                                            <Input placeholder="请输入产品编码"/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="productName" label="产品名称">
                                            <Input placeholder="请输入产品名称"/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="dateRange" label="日期范围">
                                            <RangePicker style={{width: '100%'}}/>
                                        </Form.Item>
                                    </Col>
                                </>
                            )}

                            <Col xs={24} style={{textAlign: 'right'}}>
                                <Space>
                                    <Button onClick={handleReset}>重置</Button>
                                    <Button
                                        type="primary"
                                        icon={<SearchOutlined/>}
                                        onClick={handleSearch}
                                        loading={loading}
                                    >
                                        搜索
                                    </Button>
                                </Space>
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
                        style={{marginBottom: 24}}
                        action={
                            <Button size="small" onClick={() => loadProcedures(currentPage, pageSize)}>
                                重试
                            </Button>
                        }
                    />
                )}

                {/* 工序表格 */}
                <Card style={{borderRadius: 8, border: '1px solid #d9d9d9'}}>
                    <Spin spinning={loading} tip="加载中...">
                        <Table
                            columns={columns}
                            dataSource={procedures}
                            rowKey={(record) => `${record.taskNo}_${record.procedureNo}`}
                            pagination={{
                                current: currentPage,
                                pageSize: pageSize,
                                total: total,
                                onChange: handlePaginationChange,
                                showSizeChanger: true,
                                pageSizeOptions: ['10', '20', '50', '100'],
                                showTotal: (total) => `共 ${total} 条记录`,
                                showQuickJumper: true
                            }}
                            scroll={{x: 1200}}
                            bordered
                            onRow={(record) => ({
                                onClick: () => setSelectedProcedure(record),
                                style: {
                                    cursor: 'pointer',
                                    backgroundColor: selectedProcedure?.taskNo === record.taskNo && selectedProcedure?.procedureNo === record.procedureNo ? '#f0f7ff' : 'transparent'
                                }
                            })}
                            locale={{
                                emptyText: (
                                    <div style={{textAlign: 'center', padding: 64}}>
                                        <div style={{fontSize: 48, color: '#ccc', marginBottom: 16}}>📋</div>
                                        <Text style={{fontSize: 16, color: '#999'}}>暂无工序数据</Text>
                                    </div>
                                )
                            }}
                        />
                    </Spin>
                </Card>
            </div>

            {/* 详情弹窗 */}
            <Modal
                title="工序详细信息"
                open={!!selectedProcedure}
                onCancel={() => setSelectedProcedure(null)}
                footer={[
                    <Button key="close" type="primary" onClick={() => setSelectedProcedure(null)}>
                        关闭
                    </Button>
                ]}
                width={600}
            >
                {selectedProcedure && (
                    <div style={{ padding: 16 }}>
                        <Row gutter={[16, 16]}>
                            <Col span={12}><Text strong>任务号:</Text></Col>
                            <Col span={12}>{selectedProcedure.taskNo}</Col>
                            <Col span={12}><Text strong>订单号:</Text></Col>
                            <Col span={12}>{selectedProcedure.orderNo}</Col>
                            <Col span={12}><Text strong>合同号:</Text></Col>
                            <Col span={12}>{selectedProcedure.contractNum}</Col>
                            <Col span={12}><Text strong>产品代码:</Text></Col>
                            <Col span={12}>{selectedProcedure.productCode}</Col>
                            <Col span={12}><Text strong>产品名称:</Text></Col>
                            <Col span={12}><Text ellipsis>{selectedProcedure.productName}</Text></Col>
                            <Col span={12}><Text strong>工序名称:</Text></Col>
                            <Col span={12}>{selectedProcedure.procedureName}</Col>
                            <Col span={12}><Text strong>工序号:</Text></Col>
                            <Col span={12}>{selectedProcedure.procedureNo}</Col>
                            <Col span={12}><Text strong>工序状态:</Text></Col>
                            <Col span={12}>{getStatusTag(selectedProcedure.procedureStatus, procedureStatusOptions)}</Col>
                            <Col span={12}><Text strong>任务状态:</Text></Col>
                            <Col span={12}>{getStatusTag(selectedProcedure.taskStatus, taskStatusOptions)}</Col>
                            <Col span={12}><Text strong>人工时间:</Text></Col>
                            <Col span={12}>{selectedProcedure.humanMinutes}分钟</Col>
                            <Col span={12}><Text strong>机器时间:</Text></Col>
                            <Col span={12}>{selectedProcedure.machineMinutes}分钟</Col>
                            <Col span={12}><Text strong>开始时间:</Text></Col>
                            <Col span={12}>{selectedProcedure.startTime || '-'}</Col>
                            <Col span={12}><Text strong>结束时间:</Text></Col>
                            <Col span={12}>{selectedProcedure.endTime || '-'}</Col>
                            <Col span={12}><Text strong>计划开始:</Text></Col>
                            <Col span={12}>{selectedProcedure.planStartDate}</Col>
                            <Col span={12}><Text strong>计划结束:</Text></Col>
                            <Col span={12}>{selectedProcedure.planEndDate}</Col>
                            <Col span={12}><Text strong>创建日期:</Text></Col>
                            <Col span={12}>{selectedProcedure.createDate}</Col>
                        </Row>
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default ProcedurePage;
