import React, { useState, useEffect } from 'react';
import { Table, Pagination, Button, Space, Typography, message, Form, Input, DatePicker, Select, Row, Col, Card } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { queryOrderTasksWithPagination } from '../services/orderService';
import type { OrderTask, OrderTaskQueryParams } from '../services/orderService';

const { Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const OrderQueryPage: React.FC = () => {
  const [form] = Form.useForm();
  const [orderTasks, setOrderTasks] = useState<OrderTask[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [total, setTotal] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(20);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  
  // 状态选项
  const statusOptions = [
    { label: '待生产', value: '待生产' },
    { label: '生产中', value: '生产中' },
    { label: '生产完成', value: '生产完成' },
    { label: '已暂停', value: '已暂停' },
  ];
  
  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      console.log('选中的行:', newSelectedRowKeys);
      setSelectedRowKeys(newSelectedRowKeys);
    },
  };
  
  // 分页变化处理
  const handlePageChange = (page: number, size: number) => {
    setCurrentPage(page);
    setPageSize(size);
  };

  // 查询订单任务数据
  const fetchOrders = async (params: OrderTaskQueryParams) => {
    setLoading(true);
    try {
      const response = await queryOrderTasksWithPagination(params);
      setOrderTasks(response.records || []);
      setTotal(response.total || 0);
    } catch (error) {
      message.error('网络错误，获取订单任务数据失败');
      console.error('获取订单任务数据失败:', error);
      setOrderTasks([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  // 初始加载和分页变化时获取数据
  useEffect(() => {
    const params: OrderTaskQueryParams = {
      pageNum: currentPage,
      pageSize,
      ...form.getFieldsValue(),
    };
    fetchOrders(params);
  }, [currentPage, pageSize]);

  // 处理搜索
  const handleSearch = async () => {
    const values = form.getFieldsValue();
    // 处理日期范围
    let startTime: string | undefined;
    let endTime: string | undefined;
    if (values.dateRange && values.dateRange.length === 2) {
      startTime = values.dateRange[0].format('YYYY-MM-DD');
      endTime = values.dateRange[1].format('YYYY-MM-DD');
      delete values.dateRange;
    }
    
    const params: OrderTaskQueryParams = {
      ...values,
      startTime,
      endTime,
      pageNum: 1,
      pageSize,
    };
    
    // 重置页码为1
    setCurrentPage(1);
    
    fetchOrders(params);
  };

  // 处理重置
  const handleReset = () => {
    form.resetFields();
    // 重置页码为1
    setCurrentPage(1);
    // 重新查询
    fetchOrders({
      pageNum: 1,
      pageSize,
    });
  };

  // 表格列定义
  const columns: ColumnsType<OrderTask> = [
    {
      title: '任务编号',
      dataIndex: 'taskNo',
      key: 'taskNo',
      width: 180,
    },
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
    },
    {
      title: '订单名称',
      dataIndex: 'orderName',
      key: 'orderName',
      width: 150,
    },
    {
      title: '合同编号',
      dataIndex: 'contractNum',
      key: 'contractNum',
      width: 150,
    },
    {
      title: '任务状态',
      dataIndex: 'taskStatus',
      key: 'taskStatus',
      width: 100,
    },
    {
      title: '订单状态',
      dataIndex: 'orderStatus',
      key: 'orderStatus',
      width: 100,
    },
    {
      title: '计划数量',
      dataIndex: 'planQuantity',
      key: 'planQuantity',
      width: 100,
    },
    {
      title: '订单计划数量',
      dataIndex: 'orderPlanQuantity',
      key: 'orderPlanQuantity',
      width: 120,
    },
    {
      title: '计划开始日期',
      dataIndex: 'planStartDate',
      key: 'planStartDate',
      width: 120,
    },
    {
      title: '计划结束日期',
      dataIndex: 'planEndDate',
      key: 'planEndDate',
      width: 120,
    },
    {
      title: '实际开始日期',
      dataIndex: 'factStartDate',
      key: 'factStartDate',
      width: 120,
      render: (date) => date || '-',
    },
    {
      title: '实际结束日期',
      dataIndex: 'factEndDate',
      key: 'factEndDate',
      width: 120,
      render: (date) => date || '-',
    },
    {
      title: '工艺路线',
      dataIndex: 'routeSeq',
      key: 'routeSeq',
      width: 120,
      render: (route) => route || '-',
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>订单任务查询</Title>
      <Card title="查询条件" style={{ marginBottom: 24, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)' }}>
        <Form
          form={form}
          layout="horizontal"
          labelCol={{ span: 4 }}
          wrapperCol={{ span: 10 }}
        >
          <Row gutter={[16, 24]}>
            <Col xs={8} sm={8} md={8}>
              <Form.Item name="orderName" label="单号">
                <Input placeholder="请输入订单名称" />
              </Form.Item>
            </Col>
            <Col xs={8} sm={8} md={8}>
              <Form.Item name="statusList" label="状态">
                <Select 
                  placeholder="请选择订单状态"
                  mode="multiple"
                  allowClear
                  style={{ width: '100%' }}
                >
                  {statusOptions.map(option => (
                    <Option key={option.value} value={option.value}>{option.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="dateRange" label="日期范围">
                <RangePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} style={{ textAlign: 'center', paddingTop: 8 }}>
              <Space size="middle">
                <Button type="primary" onClick={handleSearch} size="middle">
                  查询
                </Button>
                <Button onClick={handleReset} size="middle">
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>
      </Card>
      <Space style={{ marginBottom: 16 }}>
        <Button 
          type="primary" 
          disabled={selectedRowKeys.length === 0}
          onClick={() => {
            const selectedOrders = orderTasks.filter(order => 
              selectedRowKeys.includes(order.orderNo)
            );
            message.success(`已选择 ${selectedOrders.length} 条订单`);
            console.log('选中的订单数据:', selectedOrders);
          }}
        >
          批量操作
        </Button>
      </Space>
      <Table
        rowKey="taskNo"
        columns={columns}
        dataSource={orderTasks}
        rowSelection={{ type: 'checkbox', ...rowSelection }}
        loading={loading}
        pagination={false}
        scroll={{ x: 1500 }}
      />
      <Pagination
        current={currentPage}
        pageSize={pageSize}
        total={total}
        onChange={handlePageChange}
        showSizeChanger
        showQuickJumper
        showTotal={(total) => `共 ${total} 条记录`}
        style={{ marginTop: '16px', textAlign: 'right' }}
      />
    </div>
  );
};

export default OrderQueryPage;
