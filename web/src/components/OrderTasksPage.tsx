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
  Modal,
  Tag,
  Alert,
  Tooltip,
  Spin
} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import {queryTasks} from '../services/api.ts';
import type {Task, OrderTaskQueryParams} from '../services/model.ts';
import {SearchOutlined, FilterOutlined, CalendarOutlined} from '@ant-design/icons';

const {Text} = Typography;
const {RangePicker} = DatePicker;
const {Option} = Select;

const OrderTasksPage: React.FC = () => {
    const [form] = Form.useForm();
    const [orderTasks, setOrderTasks] = useState<Task[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [filterVisible, setFilterVisible] = useState<boolean>(false);
    const [selectedTask, setSelectedTask] = useState<Task | null>(null);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [pageSize, setPageSize] = useState<number>(20);
    const [total, setTotal] = useState<number>(0);

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

    // 查询订单任务数据
    const fetchTasks = useCallback(async (params: OrderTaskQueryParams, page: number = 1, size: number = 20) => {
        setLoading(true);
        setError(null);
        try {
            const response = await queryTasks({
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
        } catch {
            setError('网络错误，获取任务数据失败');
            setOrderTasks([]);
            setTotal(0);
        } finally {
            setLoading(false);
        }
    }, []);

    // 初始加载数据
    useEffect(() => {
        const params: OrderTaskQueryParams = {
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

        const params: OrderTaskQueryParams = {
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

        const params: OrderTaskQueryParams = {
            ...values,
            startTime,
            endTime,
        };

        fetchTasks(params, page, size);
    };

    // 表格列定义
    const columns: ColumnsType<Task> = [
        {
            title: '任务信息',
            dataIndex: 'taskNo',
            key: 'taskInfo',
            minWidth: 220,
            render: (_, record) => (
                <div>
                    <div style={{ fontWeight: 'bold', marginBottom: 4 }}>任务号: {record.taskNo}</div>
                    <div style={{ fontSize: 12, color: '#666' }}>订单号: {record.orderNo}</div>
                    <div style={{ fontSize: 12, color: '#666' }}>合同号: {record.contractNum}</div>
                </div>
            ),
        },
        {
            title: '产品信息',
            dataIndex: 'productName',
            key: 'productInfo',
            minWidth: 250,
            render: (_, record) => (
                <div>
                    <div style={{ fontWeight: 'bold', marginBottom: 4 }}>{record.productName}</div>
                    <div style={{ fontSize: 12, color: '#666' }}>产品代码: {record.productCode}</div>
                </div>
            ),
        },
        {
            title: '任务状态',
            dataIndex: 'taskStatus',
            key: 'taskStatus',
            minWidth: 100,
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
            minWidth: 240,
            render: (_, record) => (
                <div>
                    <div style={{ marginBottom: 4, display: 'flex', alignItems: 'center' }}>
                        <CalendarOutlined style={{ marginRight: 4, fontSize: 12 }} />
                        <Text style={{ fontSize: 12 }}>计划: {record.planStartDate} 至 {record.planEndDate}</Text>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <Text style={{ fontSize: 12 }}>数量: {record.planQuantity}</Text>
                    </div>
                </div>
            ),
        },
        {
            title: '创建信息',
            dataIndex: 'createDate',
            key: 'createInfo',
            minWidth: 220,
            render: (_, record) => (
                <div>
                    <div style={{ marginBottom: 4 }}>时间: {record.createDate}</div>
                    <div style={{ fontSize: 12, color: '#666' }}>用户: {record.createUser}</div>
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
                            onClick={() => setSelectedTask(record)}
                        >
                            详情
                        </Button>
                    </Tooltip>
                </Space>
            ),
        },
    ];

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f0f2f5' }}>
            
            {/* 主要内容 */}
            <div style={{ padding: 32 }}>  
                {/* 查询条件 */}
                <Card 
                    title={
                        <Space>
                            <FilterOutlined />
                            <Text>查询条件</Text>
                        </Space>
                    }
                    style={{ marginBottom: 24, borderRadius: 8 }}
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
                                    <Input placeholder="请输入订单编号" />
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="orderName" label="订单名称">
                                    <Input placeholder="请输入订单名称" />
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="statusList" label="任务状态">
                                    <Select
                                        placeholder="请选择任务状态"
                                        allowClear
                                        mode="multiple"
                                        style={{ width: '100%' }}
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
                                        <Form.Item name="contractNum" label="合同编号">
                                            <Input placeholder="请输入合同编号" />
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="productCode" label="产品编码">
                                            <Input placeholder="请输入产品编码" />
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="dateRange" label="日期范围">
                                            <RangePicker style={{ width: '100%' }} />
                                        </Form.Item>
                                    </Col>
                                </>
                            )}
                            
                            <Col xs={24} style={{ textAlign: 'right' }}>
                                <Space>
                                    <Button onClick={handleReset}>重置</Button>
                                    <Button 
                                        type="primary" 
                                        icon={<SearchOutlined />} 
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
                        style={{ marginBottom: 24 }}
                        action={
                            <Button size="small" onClick={() => fetchTasks({})}>
                                重试
                            </Button>
                        }
                    />
                )}
                
                {/* 任务表格 */}
                <Card style={{ borderRadius: 8, border: '1px solid #d9d9d9' }}>
                    <Spin spinning={loading} tip="加载中...">
                        <Table
                            columns={columns}
                            dataSource={orderTasks}
                            rowKey="taskNo"
                            pagination={{
                                current: currentPage,
                                pageSize: pageSize,
                                total: total,
                                showSizeChanger: true,
                                pageSizeOptions: ['10', '20', '50', '100'],
                                showTotal: (total) => `共 ${total} 条记录`,
                                showQuickJumper: true,
                                onChange: handlePaginationChange
                            }}
                            scroll={{ x: 1200 }}
                            bordered
                            onRow={(record) => ({
                                onClick: () => setSelectedTask(record),
                                style: {
                                    cursor: 'pointer',
                                    backgroundColor: selectedTask?.taskNo === record.taskNo ? '#f0f7ff' : 'transparent'
                                }
                            })}
                            locale={{
                                emptyText: (
                                    <div style={{ textAlign: 'center', padding: 64 }}>
                                        <div style={{ fontSize: 48, color: '#ccc', marginBottom: 16 }}>📋</div>
                                        <Text style={{ fontSize: 16, color: '#999' }}>暂无任务数据</Text>
                                    </div>
                                )
                            }}
                        />
                    </Spin>
                </Card>
            </div>
            
            {/* 详情弹窗 */}
            <Modal
                title="任务详细信息"
                open={!!selectedTask}
                onCancel={() => setSelectedTask(null)}
                footer={[
                    <Button key="close" type="primary" onClick={() => setSelectedTask(null)}>
                        关闭
                    </Button>
                ]}
                width={600}
            >
                {selectedTask && (
                    <div style={{ padding: 16 }}>
                        <Row gutter={[16, 16]}>
                            <Col span={12}><Text strong>任务编号:</Text></Col>
                            <Col span={12}>{selectedTask.taskNo}</Col>
                            <Col span={12}><Text strong>订单编号:</Text></Col>
                            <Col span={12}>{selectedTask.orderNo}</Col>
                            <Col span={12}><Text strong>合同编号:</Text></Col>
                            <Col span={12}>{selectedTask.contractNum}</Col>
                            <Col span={12}><Text strong>产品编码:</Text></Col>
                            <Col span={12}>{selectedTask.productCode}</Col>
                            <Col span={12}><Text strong>产品名称:</Text></Col>
                            <Col span={12}><Text ellipsis>{selectedTask.productName}</Text></Col>
                            <Col span={12}><Text strong>任务状态:</Text></Col>
                            <Col span={12}>{getStatusTag(selectedTask.taskStatus)}</Col>
                            <Col span={12}><Text strong>计划开始:</Text></Col>
                            <Col span={12}>{selectedTask.planStartDate}</Col>
                            <Col span={12}><Text strong>计划结束:</Text></Col>
                            <Col span={12}>{selectedTask.planEndDate}</Col>
                            <Col span={12}><Text strong>计划数量:</Text></Col>
                            <Col span={12}>{selectedTask.planQuantity}</Col>
                            <Col span={12}><Text strong>创建日期:</Text></Col>
                            <Col span={12}>{selectedTask.createDate}</Col>
                            <Col span={12}><Text strong>创建用户:</Text></Col>
                            <Col span={12}>{selectedTask.createUser}</Col>
                        </Row>
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default OrderTasksPage;
