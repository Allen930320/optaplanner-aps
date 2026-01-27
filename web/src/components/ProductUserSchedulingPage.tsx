import React, { useState, useCallback, useEffect } from 'react';
import { Card, message, Spin, Table, Tag, Tooltip, Form, Input, DatePicker, Row, Col, Button, Space } from 'antd';
import type { ColumnType } from 'antd/es/table';
import { queryProductUserTimeslots } from '../services/api';
import type { Timeslot } from '../services/model';
import moment from 'moment';
import { SearchOutlined, FilterOutlined } from '@ant-design/icons';

interface TaskTimeslot {
    taskNo: string;
    contractNum: string;
    productName: string;
    productCode: string;
    timeslots: Timeslot[];
}

interface TableData {
    key: string;
    taskNo: string;
    contractNum: string;
    productName: string;
    productCode: string;
    [dateKey: string]: string | Timeslot[] | undefined;
}

const ProductUserSchedulingPage: React.FC = () => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [tableData, setTableData] = useState<TableData[]>([]);
    const [dateColumns, setDateColumns] = useState<string[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [total, setTotal] = useState(0);
    const [filterVisible, setFilterVisible] = useState(false);

    const formatProcedureDetail = (timeslot: Timeslot) => {
        const { procedure, startTime, endTime, duration } = timeslot;
        const durationHours = duration ? Number((duration).toFixed(2)) : 0;
        return (
            <div style={{ fontSize: '12px', lineHeight: '1.5' }}>
                <p><strong>工序名称：</strong>{procedure?.procedureName || '未知'}（{procedure?.procedureNo || '未知'}）</p>
                <p><strong>工作中心：</strong>{procedure?.workCenter?.name || '未知'}</p>
                <p><strong>开始时间：</strong>{startTime || '未知'}</p>
                <p><strong>结束时间：</strong>{endTime || '未知'}</p>
                <p><strong>持续时间：</strong>{durationHours} 分钟</p>
                <p><strong>工序状态：</strong>{procedure?.status || '未知'}</p>
            </div>
        );
    };

    const getProcedureColor = (procedure: { procedureNo?: number } | undefined) => {
        if (!procedure) return '#f5f5f5';
        const colors = ['#e6f7ff', '#f6ffed', '#fff7e6', '#fff1f0', '#f9f0ff', '#e6fffb', '#fffbe6', '#f0f5ff'];
        const tensDigit = Math.floor((procedure.procedureNo || 0) / 10);
        return colors[tensDigit % colors.length];
    };



    const renderCellContent = (timeslots?: Timeslot[]) => {
        if (!timeslots || timeslots.length === 0) {
            return <div style={{ textAlign: 'center', padding: '8px', color: '#999' }}>未安排</div>;
        }

        return (
            <div style={{ fontSize: '11px', display: 'flex', flexDirection: 'column', gap: '2px' }}>
                {timeslots.map((ts) => (
                    <Tooltip key={ts.id} title={formatProcedureDetail(ts)} placement="topLeft">
                        <div
                            style={{
                                padding: '4px',
                                background: getProcedureColor(ts.procedure),
                                borderRadius: '4px',
                                border: '1px solid #e8e8e8',
                                cursor: 'default'
                            }}
                        >
                            <div style={{ fontWeight: 'bold', color: '#333', marginBottom: '2px' }}>
                                {ts.procedure?.procedureName || '未知'}
                            </div>
                            <Tag
                                color={
                                    ts.procedure?.status === '执行中' ? 'blue' :
                                    ts.procedure?.status === '执行完成' ? 'green' :
                                    ts.procedure?.status === '待执行' ? 'orange' : 'yellow'
                                }
                            >
                                {ts.procedure?.status || '未知'}
                            </Tag>
                        </div>
                    </Tooltip>
                ))}
            </div>
        );
    };

    const generateColumns = () => {
        const columns: ColumnType<TableData>[] = [
            {
                title: '任务信息',
                dataIndex: 'taskNo',
                key: 'taskNo',
                width: 300,
                fixed: 'left' as const,
                align: 'left' as const,
                render: (text: string, record: TableData) => (
                            <div>
                                <div style={{ fontWeight: 'bold', color: '#1890ff', marginBottom: '4px' }}>任务号: {text}</div>
                                <div style={{ fontSize: '12px', color: '#666', marginBottom: '2px' }}>
                                    合同编号: {record.contractNum || '-'}
                                </div>
                                <div style={{ fontSize: '12px', color: '#666', marginBottom: '2px' }}>
                                    产品编码: {record.productCode || '-'}
                                </div>
                                <div style={{ fontSize: '12px', color: '#666' }}>
                                    产品名称: {record.productName || '-'}
                                </div>
                            </div>
                        )
            }
        ];

        dateColumns.forEach(date => {
            columns.push({
                title: moment(date).format('M月D日'),
                dataIndex: date,
                key: date,
                width: 150,
                align: 'center' as const,
                render: (timeslots: Timeslot[]) => renderCellContent(timeslots)
            });
        });

        return columns;
    };

    const extractDates = (data: TaskTimeslot[]) => {
        const dateSet = new Set<string>();
        const today = moment().format('YYYY-MM-DD');

        data.forEach(item => {
            if (item.timeslots) {
                item.timeslots.forEach((timeslot: Timeslot) => {
                    // 获取startTime和endTime，按照优先级：timeslot > procedure > task
                    let startTime: string | null = timeslot.startTime;
                    let endTime: string | null = timeslot.endTime;
                    
                    // 如果timeslot中没有时间，尝试从procedure中获取
                    if (!startTime && !endTime) {
                        if (timeslot.procedure) {
                            startTime = timeslot.procedure.planStartDate;
                            endTime = timeslot.procedure.planEndDate;
                        }
                    }
                    
                    // 如果procedure中也没有时间，尝试从task中获取
                    if (!startTime && !endTime) {
                        if (timeslot.procedure?.task) {
                            startTime = timeslot.procedure.task.planStartDate;
                            endTime = timeslot.procedure.task.planEndDate;
                        }
                    }
                    
                    // 添加日期到集合
                    if (startTime) dateSet.add(startTime.substring(0, 10));
                    if (endTime) dateSet.add(endTime.substring(0, 10));
                });
            }
        });
        if (dateSet.size === 0) dateSet.add(today);
        return Array.from(dateSet).sort();
    };
    const buildTableData = (data: TaskTimeslot[], dates: string[]) => {
        return data.map(item => {
            const row: TableData = {
                key: item.taskNo,
                taskNo: item.taskNo,
                contractNum: item.contractNum || '',
                productName: item.productName || '',
                productCode: item.productCode || ''
            };
            dates.forEach(date => {
                row[date] = item.timeslots?.filter((ts: Timeslot) => {
                    // 获取startTime和endTime，按照优先级：timeslot > procedure > task
                    let startTime: string | null = ts.startTime;
                    let endTime: string | null = ts.endTime;
                    
                    // 如果timeslot中没有时间，尝试从procedure中获取
                    if (!startTime && !endTime) {
                        if (ts.procedure) {
                            startTime = ts.procedure.planStartDate;
                            endTime = ts.procedure.planEndDate;
                        }
                    }
                    
                    // 如果procedure中也没有时间，尝试从task中获取
                    if (!startTime && !endTime) {
                        if (ts.procedure?.task) {
                            startTime = ts.procedure.task.planStartDate;
                            endTime = ts.procedure.task.planEndDate;
                        }
                    }
                    
                    // 检查是否有有效的时间数据（只要有startTime或endTime中的任意一个即可）
                    if (!startTime && !endTime) return false;
                    
                    // 检查时间槽是否与当前日期重叠
                    let startDate = null;
                    let endDate = null;
                    
                    if (startTime) startDate = startTime.substring(0, 10);
                    if (endTime) endDate = endTime.substring(0, 10);
                    
                    // 处理只有startTime的情况
                    if (startDate && !endDate) {
                        return startDate === date;
                    }
                    
                    // 处理只有endTime的情况
                    if (!startDate && endDate) {
                        return endDate === date;
                    }
                    
                    // 处理两者都有的情况
                    if (startDate && endDate) {
                        // 处理endTime早于startTime的情况
                        if (startDate > endDate) {
                            return startDate === date || endDate === date;
                        }
                        return startDate <= date && endDate >= date;
                    }
                    
                    return false;
                }) || [];
            });

            return row;
        });
    };
    const fetchData = useCallback(async (params?: {
        taskNo?: string;
        productName?: string;
        productCode?: string;
        contractNum?: string;
        startTime?: string;
        endTime?: string;
        pageNum?: number;
        pageSize?: number;
    }) => {
        setLoading(true);
        try {
            const response = await queryProductUserTimeslots({
                taskNo: params?.taskNo || '',
                productName: params?.productName || '',
                productCode: params?.productCode || '',
                contractNum: params?.contractNum || '',
                startTime: params?.startTime || '',
                endTime: params?.endTime || '',
                pageNum: params?.pageNum || currentPage,
                pageSize: params?.pageSize || pageSize
            });

            if (response.code === 200 && response.data) {
                const data = response.data.content || [];
                setTotal(response.data.totalElements || 0);
                const dates = extractDates(data);
                setDateColumns(dates);
                setTableData(buildTableData(data, dates));
            } else {
                message.error(response.msg || '查询失败');
            }
        } catch {
            message.error('查询失败');
        } finally {
            setLoading(false);
        }
    }, [currentPage, pageSize]);
    const handleSearch = async () => {
        const values = form.getFieldsValue();
        let startTime: string | undefined;
        let endTime: string | undefined;
        if (values.dateRange && values.dateRange.length === 2) {
            startTime = values.dateRange[0].format('YYYY-MM-DD');
            endTime = values.dateRange[1].format('YYYY-MM-DD');
        }
        setCurrentPage(1);
        await fetchData({
            ...values,
            startTime,
            endTime,
            pageNum: 1,
            pageSize
        });
    };
    const handleReset = async () => {
        form.resetFields();
        setCurrentPage(1);
        await fetchData({
            pageNum: 1,
            pageSize
        });
    };
    const handlePaginationChange = async (page: number, size: number) => {
        setCurrentPage(page);
        setPageSize(size);
        const values = form.getFieldsValue();
        let startTime: string | undefined;
        let endTime: string | undefined;
        if (values.dateRange && values.dateRange.length === 2) {
            startTime = values.dateRange[0].format('YYYY-MM-DD');
            endTime = values.dateRange[1].format('YYYY-MM-DD');
        }

        await fetchData({
            ...values,
            startTime,
            endTime,
            pageNum: page,
            pageSize: size
        });
    };
    useEffect(() => {
        fetchData({
            pageNum: currentPage,
            pageSize
        });
    }, []);

    return (
        <div style={{ padding: '20px', minHeight: '100vh', backgroundColor: '#f0f2f5' }}>
            <h1 style={{ marginBottom: '20px', fontSize: '20px', color: '#262626' }}>生产人员专用调度</h1>

            <Card
                title={
                    <Space>
                        <FilterOutlined />
                        <span>查询条件</span>
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
                            <Form.Item name="taskNo" label="任务编号">
                                <Input placeholder="请输入任务编号" />
                            </Form.Item>
                        </Col>
                        <Col xs={24} sm={12} md={8} lg={6}>
                            <Form.Item name="productName" label="产品名称">
                                <Input placeholder="请输入产品名称" />
                            </Form.Item>
                        </Col>
                        <Col xs={24} sm={12} md={8} lg={6}>
                            <Form.Item name="productCode" label="产品编码">
                                <Input placeholder="请输入产品编码" />
                            </Form.Item>
                        </Col>

                        {filterVisible && (
                            <>
                                <Col xs={24} sm={12} md={8} lg={6}>
                                    <Form.Item name="contractNum" label="合同编号">
                                        <Input placeholder="请输入合同编号" />
                                    </Form.Item>
                                </Col>
                                <Col xs={24} sm={24} md={16} lg={12}>
                                    <Form.Item name="dateRange" label="日期范围">
                                        <DatePicker.RangePicker style={{ width: '100%' }} />
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

            <Spin spinning={loading}>
                <Card>
                    <Table
                        columns={generateColumns()}
                        dataSource={tableData}
                        scroll={{ x: 'max-content', y: 600 }}
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
                        size="middle"
                        bordered
                        rowKey="key"
                    />
                </Card>
            </Spin>
        </div>
    );
};

export default ProductUserSchedulingPage;
