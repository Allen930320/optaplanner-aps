import React, {useState, useEffect, useCallback} from 'react';
import {
  Table,
  Typography,
  Form,
  Input,
  DatePicker,
  Select,
  Row,
  Col,
  Card,
  Space,
  Button,
  Tag,
  Alert,
  Spin,
  message,
  Modal
} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import {queryApsOrderTaskForPage, startTasks, setEndDate} from '../services/api.ts';
import {SearchOutlined, FilterOutlined, PlayCircleOutlined} from '@ant-design/icons';
import dayjs from 'dayjs';
import type {OrderTask} from "../services/model.ts";

const {Text} = Typography;
const {RangePicker} = DatePicker;
const {Option} = Select;

const ApsOrderTasksPage: React.FC = () => {
    const [form] = Form.useForm();
    const [orderTasks, setOrderTasks] = useState<any[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [filterVisible, setFilterVisible] = useState<boolean>(false);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [pageSize, setPageSize] = useState<number>(20);
    const [total, setTotal] = useState<number>(0);
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const [schedulingLoading, setSchedulingLoading] = useState<boolean>(false);
    
    // 弹窗相关状态
    const [modalVisible, setModalVisible] = useState<boolean>(false);
    const [currentTask, setCurrentTask] = useState<any>(null);
    const [modalLoading, setModalLoading] = useState<boolean>(false);
    const [planStartDate, setPlanStartDate] = useState<any>(null);
    const [planEndDate, setPlanEndDate] = useState<any>(null);

    // 状态选项
    const statusOptions = [
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

    // 获取状态标签
    const getStatusTag = (status: string) => {
        const option = statusOptions.find(opt => opt.value === status);
        if (option) {
            return <Tag color={option.color}>{option.label}</Tag>;
        }
        return <Tag>{status}</Tag>;
    };

    // 查询待排产任务数据
    const fetchTasks = useCallback(async (params: any, page: number = 1, size: number = 20) => {
        setLoading(true);
        setError(null);
        try {
            const response = await queryApsOrderTaskForPage({
                ...params,
                pageNum: page,
                pageSize: size
            });
            if (response && response.code === 200) {
                setOrderTasks(response.data?.content || []);
                setTotal(response.data?.totalElements || 0);
            } else {
                setOrderTasks([]);
                setTotal(0);
            }
        } catch (err) {
            setError('网络错误，获取任务数据失败');
            setOrderTasks([]);
            setTotal(0);
        } finally {
            setLoading(false);
        }
    }, []);

    // 初始加载数据
    useEffect(() => {
        const params: any = {
            ...form.getFieldsValue(),
        };
        fetchTasks(params, currentPage, pageSize);
    }, [form, fetchTasks, currentPage, pageSize]);

    // 处理搜索
    const handleSearch = async () => {
        const values = form.getFieldsValue();
        // 处理日期范围
        let startTime: string | undefined;
        let endTime: string | undefined;
        if (values.dateRange && values.dateRange.length === 2) {
            startTime = values.dateRange[0].format('YYYY-MM-DD');
            endTime = values.dateRange[1].format('YYYY-MM-DD');
        }

        const params: any = {
            ...values,
            startTime,
            endTime,
        };

        // 搜索时重置到第一页
        setCurrentPage(1);
        fetchTasks(params, 1, pageSize);
    };

    // 处理重置
    const handleReset = () => {
        form.resetFields();
        // 重置到第一页
        setCurrentPage(1);
        // 重新查询
        fetchTasks({}, 1, pageSize);
    };

    // 处理分页
    const handlePaginationChange = (page: number, size: number) => {
        setCurrentPage(page);
        setPageSize(size);
        // 重新查询数据
        const values = form.getFieldsValue();
        // 处理日期范围
        let startTime: string | undefined;
        let endTime: string | undefined;
        if (values.dateRange && values.dateRange.length === 2) {
            startTime = values.dateRange[0].format('YYYY-MM-DD');
            endTime = values.dateRange[1].format('YYYY-MM-DD');
        }

        const params: any = {
            ...values,
            startTime,
            endTime,
        };

        fetchTasks(params, page, size);
    };

    // 处理执行排产
    const handleExecuteScheduling = async () => {
        if (selectedRowKeys.length === 0) {
            message.warning('请至少选择一个任务');
            return;
        }

        // 从选中的行中获取任务编号
        const selectedTasks = orderTasks.filter(task => 
            selectedRowKeys.includes(task.taskNo)
        );
        const taskNos = selectedTasks.map(task => task.taskNo);

        setSchedulingLoading(true);
        try {
            await startTasks(taskNos);
            message.success('排产执行成功');
            // 排产成功后清空选择
            setSelectedRowKeys([]);
            // 刷新列表数据
            const values = form.getFieldsValue();
            // 处理日期范围
            let startTime: string | undefined;
            let endTime: string | undefined;
            if (values.dateRange && values.dateRange.length === 2) {
                startTime = values.dateRange[0].format('YYYY-MM-DD');
                endTime = values.dateRange[1].format('YYYY-MM-DD');
            }

            const params: any = {
                ...values,
                startTime,
                endTime,
            };

            fetchTasks(params, currentPage, pageSize);
        } catch (error) {
            message.error(`排产执行失败: ${(error as Error).message}`);
        } finally {
            setSchedulingLoading(false);
        }
    };

    // 打开修改计划时间弹窗
    const handleOpenEndDateModal = (record: any) => {
        // 先关闭弹窗
        setModalVisible(false);
        
        // 重置状态
        setCurrentTask(null);
        setPlanStartDate(null);
        setPlanEndDate(null);
        
        // 延迟设置新任务和打开弹窗
        setTimeout(() => {
            // 设置当前任务
            setCurrentTask(record);
            // 初始化计划时间
            if (record.planStartDate) {
                setPlanStartDate(dayjs(record.planStartDate));
            } else {
                setPlanStartDate(null);
            }
            if (record.planEndDate) {
                setPlanEndDate(dayjs(record.planEndDate));
            } else {
                setPlanEndDate(null);
            }
            // 打开弹窗
            setModalVisible(true);
        }, 100);
    };

    // 关闭弹窗
    const handleCloseModal = () => {
        setCurrentTask(null);
        setPlanStartDate(null);
        setPlanEndDate(null);
        setModalVisible(false);
    };

    // 提交修改计划时间
    const handleSubmitEndDate = async () => {
        if (!currentTask || !planStartDate || !planEndDate) {
            message.warning('请选择计划开始时间和完成时间');
            return;
        }

        setModalLoading(true);
        try {
            let startDateStr: string;
            let endDateStr: string;
            
            // 处理计划开始时间
            if (planStartDate.format) {
                startDateStr = planStartDate.format('YYYY-MM-DD');
            } else if (planStartDate._isAMomentObject) {
                startDateStr = planStartDate.format('YYYY-MM-DD');
            } else {
                startDateStr = String(planStartDate);
            }
            
            // 处理计划完成时间
            if (planEndDate.format) {
                endDateStr = planEndDate.format('YYYY-MM-DD');
            } else if (planEndDate._isAMomentObject) {
                endDateStr = planEndDate.format('YYYY-MM-DD');
            } else {
                endDateStr = String(planEndDate);
            }
            
            await setEndDate(currentTask.taskNo, startDateStr, endDateStr);
            message.success('计划时间修改成功');
            handleCloseModal();
            // 刷新列表数据
            const values = form.getFieldsValue();
            // 处理日期范围
            let startTime: string | undefined;
            let endTime: string | undefined;
            if (values.dateRange && values.dateRange.length === 2) {
                startTime = values.dateRange[0].format('YYYY-MM-DD');
                endTime = values.dateRange[1].format('YYYY-MM-DD');
            }

            const params: any = {
                ...values,
                startTime,
                endTime,
            };

            fetchTasks(params, currentPage, pageSize);
        } catch (error) {
            message.error(`修改计划时间失败: ${(error as Error).message}`);
        } finally {
            setModalLoading(false);
        }
    };

    // 表格列定义
    const columns: ColumnsType<OrderTask> = [
        {
            title: '任务信息',
            dataIndex: 'taskNo',
            key: 'taskInfo',
            minWidth: 160,
            render: (_, record) => (
                <div>
                    <div style={{ fontSize: 13, fontWeight: 'bold' }}>任务号: {record.taskNo}</div>
                    <div style={{ fontSize: 11, color: '#666', marginTop: 2 }}>订单号: {record.orderNo}</div>
                    <div style={{ fontSize: 11, color: '#666', marginTop: 1 }}>合同号: {record.contractNum}</div>
                </div>
            ),
        },
        {
            title: '产品信息',
            dataIndex: 'productName',
            key: 'productInfo',
            minWidth: 180,
            render: (_, record) => (
                <div>
                    <div style={{ fontSize: 13, fontWeight: 'bold' }}>{record.productName}</div>
                    <div style={{ fontSize: 11, color: '#666', marginTop: 2 }}>产品代码: {record.productCode}</div>
                </div>
            ),
        },
        {
            title: '任务状态',
            dataIndex: 'taskStatus',
            key: 'taskStatus',
            minWidth: 80,
            render: (_, record) => (
                <div>
                    {getStatusTag(record.taskStatus)}
                </div>
            ),
        },
        {
            title: '计划信息',
            dataIndex: 'planStartDate',
            key: 'planInfo',
            minWidth: 200,
            render: (_, record) => (
                <div style={{ cursor: 'pointer' }} onClick={() => handleOpenEndDateModal(record)}>
                    <div style={{ fontSize: 11, marginBottom: 2 }}>计划: {record.planStartDate} 至 {record.planEndDate}</div>
                    <div style={{ fontSize: 11, marginBottom: 2 }}>实际: {record.factStartDate || '未开始'} 至 {record.factEndDate || '未完成'}</div>
                    <div style={{ fontSize: 11 }}>数量: {record.planQuantity}</div>
                </div>
            ),
        },
        {
            title: '创建信息',
            dataIndex: 'createDate',
            key: 'createInfo',
            minWidth: 160,
            render: (_, record) => (
                <div>
                    <div style={{ fontSize: 11, marginBottom: 2 }}>时间: {record.createDate}</div>
                    <div style={{ fontSize: 11, color: '#666' }}>用户: {record.createUser}</div>
                </div>
            ),
        },
    ];

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f0f2f5' }}>
            
            {/* 主要内容 */}
            <div style={{ padding: 2 }}>
                {/* 查询条件 */}
                <Card 
                    title={
                        <Space>
                            <FilterOutlined />
                            <Text>查询条件</Text>
                        </Space>
                    }
                    style={{ marginBottom: 6, borderRadius: 6, boxShadow: '0 1px 3px rgba(0,0,0,0.1)', padding: '8px 6px' }}
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
                                    <Input placeholder="请输入订单编号" size="small" style={{ height: 24 }} />
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="orderName" label="订单名称" style={{ marginBottom: 4 }}>
                                    <Input placeholder="请输入订单名称" size="small" style={{ height: 24 }} />
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="statusList" label="任务状态" style={{ marginBottom: 4 }}>
                                    <Select
                                        placeholder="请选择任务状态"
                                        allowClear
                                        mode="multiple"
                                        style={{ width: '100%', height: 24 }}
                                        size="small"
                                    >
                                        {statusOptions.map(option => (
                                            <Option key={option.value} value={option.value}>{option.label}</Option>
                                        ))}
                                    </Select>
                                </Form.Item>
                            </Col>
                            
                            {filterVisible && (
                                <>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="contractNum" label="合同编号" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入合同编号" size="small" style={{ height: 24 }} />
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="productCode" label="产品编码" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入产品编码" size="small" style={{ height: 24 }} />
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="dateRange" label="日期范围" style={{ marginBottom: 4 }}>
                                            <RangePicker style={{ width: '100%', height: 24 }} size="small" />
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
                                            icon={<SearchOutlined />} 
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
                        style={{ marginBottom: 16 }}
                        action={
                            <Button size="small" onClick={() => fetchTasks({})}>
                                重试
                            </Button>
                        }
                    />
                )}
                
                {/* 任务表格 */}
                <Card 
                    style={{ borderRadius: 6, border: '1px solid #d9d9d9', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}
                >
                    <Spin spinning={loading} tip="加载中...">
                        {/* 操作按钮区域 - 位于列表块内部 */}
                        <div style={{ marginBottom: 12, padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                            <Button 
                                type="primary" 
                                size="small"
                                icon={<PlayCircleOutlined />}
                                onClick={handleExecuteScheduling}
                                loading={schedulingLoading}
                                disabled={selectedRowKeys.length === 0}
                            >
                                执行排产
                            </Button>
                        </div>
                        
                        {/* 修改计划时间弹窗 */}
                        <Modal
                            title="修改计划时间"
                            open={modalVisible}
                            onCancel={handleCloseModal}
                            onOk={handleSubmitEndDate}
                            okText="确定"
                            cancelText="取消"
                            confirmLoading={modalLoading}
                            width={500}
                        >
                            <div style={{ padding: '20px 0' }}>
                                <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center' }}>
                                    <span style={{ display: 'inline-block', width: 90, fontWeight: 500 }}>任务号:</span>
                                    <span style={{ flex: 1, wordBreak: 'break-all' }}>{currentTask?.taskNo}</span>
                                </div>
                                <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center' }}>
                                    <span style={{ display: 'inline-block', width: 90, fontWeight: 500 }}>产品名称:</span>
                                    <span style={{ flex: 1, wordBreak: 'break-all' }}>{currentTask?.productName}</span>
                                </div>
                                <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center' }}>
                                    <span style={{ display: 'inline-block', width: 90, fontWeight: 500 }}>计划开始时间:</span>
                                    <DatePicker
                                        style={{ width: 220 }}
                                        format='YYYY-MM-DD'
                                        value={planStartDate}
                                        onChange={(date) => setPlanStartDate(date)}
                                    />
                                </div>
                                <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center' }}>
                                    <span style={{ display: 'inline-block', width: 90, fontWeight: 500 }}>计划完成时间:</span>
                                    <DatePicker
                                        style={{ width: 220 }}
                                        format='YYYY-MM-DD'
                                        value={planEndDate}
                                        onChange={(date) => setPlanEndDate(date)}
                                    />
                                </div>
                            </div>
                        </Modal>
                        
                        <Table
                            columns={columns}
                            dataSource={orderTasks}
                            rowKey="taskNo"
                            rowSelection={{
                                selectedRowKeys,
                                onChange: (keys) => setSelectedRowKeys(keys),
                                selections: [
                                    Table.SELECTION_ALL,
                                    Table.SELECTION_INVERT,
                                    Table.SELECTION_NONE
                                ],
                                getCheckboxProps: (record) => ({
                                    disabled: record.lockedRemark === 'true'
                                })
                            }}
                            pagination={{
                                current: currentPage,
                                pageSize: pageSize,
                                total: total,
                                showSizeChanger: true,
                                pageSizeOptions: ['10', '20', '50', '100'],
                                showTotal: (total) => `共 ${total} 条记录`,
                                showQuickJumper: true,
                                onChange: handlePaginationChange,
                                size: 'small'
                            }}
                            scroll={{ x: 900 }}
                            bordered
                            size="small"
                            locale={{
                                emptyText: (
                                    <div style={{ textAlign: 'center', padding: 32 }}>
                                        <div style={{ fontSize: 32, color: '#ccc', marginBottom: 12 }}>📋</div>
                                        <Text style={{ fontSize: 14, color: '#999' }}>暂无任务数据</Text>
                                    </div>
                                )
                            }}
                        />
                    </Spin>
                </Card>
            </div>
        </div>
    );
};

export default ApsOrderTasksPage;
