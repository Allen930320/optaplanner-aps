import React, {useState, useEffect, useCallback} from 'react';
import {
  Table,
  Button,
  Space,
  Typography,
  Form,
  Input,
  Row,
  Col,
  Card,
  Spin,
  Alert,
  message
} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import {queryRouteProcedurePage} from '../services/api.ts';
import type {RouteProcedureQueryDTO} from '../services/model.ts';
import {SearchOutlined, FilterOutlined} from '@ant-design/icons';

const {Text} = Typography;

const RouteProcedurePage: React.FC = () => {
    // 状态管理
    const [form] = Form.useForm();
    const [routeProcedures, setRouteProcedures] = useState<RouteProcedureQueryDTO[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [pageSize, setPageSize] = useState<number>(20);
    const [total, setTotal] = useState<number>(0);
    const [filterVisible, setFilterVisible] = useState<boolean>(false);

    // 加载工艺路线数据
    const loadRouteProcedures = useCallback(async (page: number = 1, size: number = 20) => {
        setLoading(true);
        setError(null);
        try {
            const values = form.getFieldsValue();
            const response = await queryRouteProcedurePage({
                productName: values.productName,
                productCode: values.productCode,
                orderNo: values.orderNo,
                taskNo: values.taskNo,
                contractNum: values.contractNum,
                pageNum: page,
                pageSize: size
            });
            if (response.code === 200) {
                const routeProcedureList = response.data?.content || [];
                setRouteProcedures(routeProcedureList);
                setTotal(response.data?.totalElements || 0);
                setCurrentPage(page);
                setPageSize(size);
            } else {
                setError(response.msg || '获取数据失败');
                setRouteProcedures([]);
                setTotal(0);
            }
        } catch (err) {
            setError('网络错误，请稍后重试');
            setRouteProcedures([]);
            setTotal(0);
        } finally {
            setLoading(false);
        }
    }, [form]);

    // 初始加载
    useEffect(() => {
        loadRouteProcedures();
    }, [loadRouteProcedures]);

    // 处理搜索
    const handleSearch = () => {
        loadRouteProcedures(1, pageSize);
    };

    // 处理重置
    const handleReset = () => {
        form.resetFields();
        loadRouteProcedures(1, pageSize);
    };

    // 处理分页
    const handlePaginationChange = (page: number, size: number) => {
        loadRouteProcedures(page, size);
    };

    // 表格列定义
    const columns: ColumnsType<RouteProcedureQueryDTO> = [
        {
            title: '工艺信息',
            dataIndex: 'routeName',
            key: 'routeInfo',
            minWidth: 160,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 13, fontWeight: 'bold'}}>名称: {record.routeName}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 2}}>版本: {record.routeCode}-{record.productVersion}-{record.routeVersion}</div>
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
                    <div style={{fontSize: 11, color: '#666', marginTop: 2}}>序号: {record.procedureNo}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 1}}>类型: {record.procedureType}</div>
                    <div style={{fontSize: 11, color: '#666', marginTop: 1}}>内容: {record.procedureContent}</div>
                </div>
            ),
        },
        {
            title: '工时信息',
            dataIndex: 'machineHours',
            key: 'workHours',
            minWidth: 120,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11, marginBottom: 1}}>机器: {record.machineHours || 0} 小时</div>
                    <div style={{fontSize: 11, marginBottom: 1}}>人工: {record.humanHours || 0} 小时</div>
                    <div style={{fontSize: 11}}>预计天数: {record.days || 0} 天</div>
                </div>
            ),
        },
        {
            title: '责任信息',
            dataIndex: 'dutyUser',
            key: 'dutyInfo',
            minWidth: 120,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11, marginBottom: 1}}>责任用户: {record.dutyUser}</div>
                    <div style={{fontSize: 11, marginBottom: 1}}>创建用户: {record.createUser}</div>
                    <div style={{fontSize: 11}}>更新用户: {record.updateUser}</div>
                </div>
            ),
        },
        {
            title: '时间信息',
            dataIndex: 'createDate',
            key: 'timeInfo',
            minWidth: 140,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11, marginBottom: 1}}>创建: {record.createDate}</div>
                    <div style={{fontSize: 11}}>更新: {record.updateDate}</div>
                </div>
            ),
        },
        {
            title: '其他信息',
            dataIndex: 'remark',
            key: 'otherInfo',
            minWidth: 120,
            render: (_, record) => (
                <div>
                    <div style={{fontSize: 11}}>备注: {record.remark || '-'}</div>
                </div>
            ),
        },
    ];

    return (
        <div style={{minHeight: '100vh', backgroundColor: '#f0f2f5'}}>
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
                                <Form.Item name="productName" label="产品名称" style={{ marginBottom: 4 }}>
                                    <Input placeholder="请输入产品名称" size="small" style={{ height: 24 }}/>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="productCode" label="产品编码" style={{ marginBottom: 4 }}>
                                    <Input placeholder="请输入产品编码" size="small" style={{ height: 24 }}/>
                                </Form.Item>
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <Form.Item name="orderNo" label="订单编号" style={{ marginBottom: 4 }}>
                                    <Input placeholder="请输入订单编号" size="small" style={{ height: 24 }}/>
                                </Form.Item>
                            </Col>

                            {filterVisible && (
                                <>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="taskNo" label="任务编号" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入任务编号" size="small" style={{ height: 24 }}/>
                                        </Form.Item>
                                    </Col>
                                    <Col xs={24} sm={12} md={8} lg={6}>
                                        <Form.Item name="contractNum" label="合同编号" style={{ marginBottom: 4 }}>
                                            <Input placeholder="请输入合同编号" size="small" style={{ height: 24 }}/>
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
                            <Button size="small" onClick={() => loadRouteProcedures(currentPage, pageSize)}>
                                重试
                            </Button>
                        }
                    />
                )}

                {/* 工艺路线表格 */}
                <Card style={{borderRadius: 6, border: '1px solid #d9d9d9', boxShadow: '0 1px 3px rgba(0,0,0,0.1)'}}>
                    <Spin spinning={loading} tip="加载中...">
                        <Table
                            columns={columns}
                            dataSource={routeProcedures}
                            rowKey={(record) => record.id}
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
                            scroll={{x: 1200}}
                            bordered
                            size="small"
                            locale={{
                                emptyText: (
                                    <div style={{textAlign: 'center', padding: 32}}>
                                        <div style={{fontSize: 32, color: '#ccc', marginBottom: 12}}>📋</div>
                                        <Text style={{fontSize: 14, color: '#999'}}>暂无工艺路线数据</Text>
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

export default RouteProcedurePage;