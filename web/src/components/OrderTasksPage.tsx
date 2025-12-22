import React, {useState, useEffect, useCallback} from 'react';
import { Table, Button, Space, Typography, message, Form, Input, DatePicker, Select, Row, Col, Card, Tag, Modal, InputNumber } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { queryTasks, startTasks, createTimeslot, getTimeslotByTaskNo } from '../services/api.ts';
import type { Task, OrderTaskQueryParams } from '../services/model.ts';
import type { Timeslot } from '../services/model';
import moment from 'moment';

const { Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const OrderTasksPage: React.FC = () => {
  const [form] = Form.useForm();
  const [orderTasks, setOrderTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  
  // 创建时间槽相关状态
  const [createTimeslotModalVisible, setCreateTimeslotModalVisible] = useState<boolean>(false);
  const [createTimeslotByProcedureModalVisible, setCreateTimeslotByProcedureModalVisible] = useState<boolean>(false);
  const [timeslotForm] = Form.useForm();
  const [creatingTimeslot, setCreatingTimeslot] = useState<boolean>(false);
  
  // 时间槽列表模态框状态
  const [timeslotListModalVisible, setTimeslotListModalVisible] = useState(false);
  const [timeslotData, setTimeslotData] = useState<Timeslot[]>([]);
  const [timeslotLoading, setTimeslotLoading] = useState(false);
  const [currentTaskNo, setCurrentTaskNo] = useState('');
  const [selectedTimeslotKeys, setSelectedTimeslotKeys] = useState<string[]>([]);
  
  // 处理创建时间槽
  const handleCreateTimeslot = async () => {
    try {
      const values = await timeslotForm.validateFields();
      
      setCreatingTimeslot(true);
      
      // 提取选中的任务编号
      const taskNos = selectedRowKeys.map(key => String(key));
      
      // 调用API，空数组作为procedureIds参数
      // 确保只传递实际存在的值，使用默认值处理未定义的情况
      const timeValue = values.time !== null && values.time !== undefined ? values.time : 0.5;
      const sliceValue = values.slice !== null && values.slice !== undefined ? values.slice : 0;
      await createTimeslot(taskNos, [], timeValue, sliceValue);
      message.success('时间槽创建成功');
      setCreateTimeslotModalVisible(false);
      timeslotForm.resetFields();
    } catch {
      message.error('时间槽创建失败，请重试');
    } finally {
      setCreatingTimeslot(false);
    }
  };
  
  // 状态选项
  const statusOptions = [
    { label: '待生产', value: '待生产' },
    { label: '生产中', value: '生产中' },
    { label: '生产完成', value: '生产完成' },
    { label: '已暂停', value: '已暂停' },
  ];
  
  // 时间槽表格列定义
  const timeslotColumns: ColumnsType<Timeslot> = [
    {
      title: '工序名称',
      dataIndex: ['procedure', 'procedureName'],
      key: 'procedureName',
      width: 120,
    },
    {
      title: '工序编号',
      dataIndex: ['procedure', 'procedureNo'],
      key: 'procedureNo',
      width: 100,
    },
    {
      title: '工作中心',
      dataIndex: ['workCenter', 'name'],
      key: 'workCenterName',
      width: 200,
    },
    {
      title: '机器时间(分钟)',
      dataIndex: 'duration',
      key: 'duration',
      width: 120,
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: (text: string) => text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : '',
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180,
      render: (text: string) => text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : '',
    },
    {
      title: '工序状态',
      dataIndex: ['procedure', 'status'],
      key: 'procedureStatus',
      width: 100,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
    },
    {
      title: '并行',
      dataIndex: 'parallel',
      key: 'parallel',
      width: 80,
      render: (text: boolean) => text ? '是' : '否',
    },
  ];
  
  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {

      setSelectedRowKeys(newSelectedRowKeys);
    },
  };

  // 查询订单任务数据
  const fetchTasks = useCallback(async (params: OrderTaskQueryParams) => {
    setLoading(true);
    try {
      const response = await queryTasks(params);
      setOrderTasks(response || []);
    } catch {
      message.error('网络错误，获取任务数据失败');
      setOrderTasks([]);
    } finally {
      setLoading(false);
    }
  }, [setOrderTasks, setLoading]);

  // 初始加载数据
  useEffect(() => {
    const params: OrderTaskQueryParams = {
      ...form.getFieldsValue(),
    };
    fetchTasks(params);
  }, [form, fetchTasks]);

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
    };
    
    fetchTasks(params);
  };

  // 处理重置
  const handleReset = () => {
    form.resetFields();
    // 重新查询
    fetchTasks({
    });
  };

  // 处理开始任务
  const handleStartTasks = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要开始的任务');
      return;
    }

    try {
      // 提取选中任务的订单编号
      const taskNos = selectedRowKeys.map(key => {
        const task = orderTasks.find(t => t.taskNo === key);
        return task?.taskNo;
      }).filter(Boolean) as string[];

      // 调用API服务中的startTasks函数并获取返回信息
      const messageInfo = await startTasks(taskNos);
      // 使用返回的信息显示提示框
      message.success(messageInfo || '任务开始成功');
    } catch {
      message.error('任务开始失败，请重试');
    }
  };
  
  // 获取时间槽列表
  const handleGetTimeslotList = async (taskNo: string) => {
    setCurrentTaskNo(taskNo);
    setTimeslotLoading(true);
    try {
      const response = await getTimeslotByTaskNo(taskNo);
      // 假设API返回的是响应对象，直接检查data是否存在
      if (response.data) {
        setTimeslotData(response.data);
        setTimeslotListModalVisible(true);
        // 重置选中的时间槽
        setSelectedTimeslotKeys([]);
      } else {
        message.error('获取时间槽列表失败');
      }
    } catch {
      message.error('获取时间槽列表失败，请重试');
    } finally {
      setTimeslotLoading(false);
    }
  };
  
  // 基于时间槽ID创建时间槽 - 显示模态框
  const handleCreateTimeslotByTimeslotIds = () => {
    if (selectedTimeslotKeys.length === 0) {
      message.warning('请选择要创建时间槽的时间槽');
      return;
    }
    
    // 显示模态框让用户输入时间或分片信息
    setCreateTimeslotByProcedureModalVisible(true);
  };
  
  // 基于时间槽ID创建时间槽 - 实际执行API调用
  const handleCreateTimeslotByTimeslotIdsConfirm = async () => {
    try {
      // 直接使用选中的时间槽ID作为timeslotIds参数
      const timeslotIds = selectedTimeslotKeys;
      
      const values = await timeslotForm.validateFields();
      
      setCreatingTimeslot(true);
      
      // 调用API，空数组作为taskNos参数，timeslotIds为选中的时间槽ID
      const timeValue = values.time !== null && values.time !== undefined ? values.time : 0.5;
      const sliceValue = values.slice !== null && values.slice !== undefined ? values.slice : 0;
      await createTimeslot([], timeslotIds, timeValue, sliceValue);
      message.success('时间槽创建成功');
      
      // 关闭模态框
      setCreateTimeslotByProcedureModalVisible(false);
      
      // 刷新时间槽列表
      const response = await getTimeslotByTaskNo(currentTaskNo);
      if (response.data) {
        setTimeslotData(response.data);
      }
      
      // 重置选中的时间槽和表单
      setSelectedTimeslotKeys([]);
      timeslotForm.resetFields();
    } catch {
      message.error('暂停任务失败，请重试');
    } finally {
      setCreatingTimeslot(false);
    }
  };

  // 表格列定义
  const columns: ColumnsType<Task> = [
    {
      title: '任务编号',
      dataIndex: 'taskNo',
      key: 'taskNo',
      width: 180,
      render: (text: string) => (
        <span style={{ cursor: 'pointer', color: '#1890ff' }}
          onClick={() => handleGetTimeslotList(text)}
          onMouseEnter={(e) => {
              const target = e.target as HTMLElement;
              target.style.textDecoration = 'underline';
            }}
            onMouseLeave={(e) => {
              const target = e.target as HTMLElement;
              target.style.textDecoration = 'none';
            }}
        >
          {text}
        </span>
      ),
    },
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority: number) => {
        // 根据优先级数字返回不同深浅的颜色
        let color = 'default'; // 默认灰色
        
        if (priority > 0) {
          // 优先级数字越大，颜色越深
          if (priority <= 20) {
            color = 'blue';
          } else if (priority <= 40) {
            color = 'cyan';
          } else if (priority <= 60) {
            color = 'green';
          } else if (priority <= 80) {
            color = 'orange';
          } else {
            color = 'red';
          }
        }
        
        return <Tag color={color}>{priority}</Tag>;
      },
    },
    {
      title: '计划开始日期',
      dataIndex: 'planStartDate',
      key: 'planStartDate',
      width: 150,
      render: (text: string) => text ? text.split('T')[0] : '',
    },
    {
      title: '计划结束日期',
      dataIndex: 'planEndDate',
      key: 'planEndDate',
      width: 150,
      render: (text: string) => text ? text.split('T')[0] : '',
    },
    {
      title: '实际开始日期',
      dataIndex: 'factStartDate',
      key: 'factStartDate',
      width: 150,
      render: (text: string | null) => text ? text.split('T')[0] : '',
    },
    {
      title: '实际结束日期',
      dataIndex: 'factEndDate',
      key: 'factEndDate',
      width: 150,
      render: (text: string | null) => text ? text.split('T')[0] : '',
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>任务列表</Title>
      <Card title="查询条件" style={{ marginBottom: 24, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)' }}>
        <Form
          form={form}
          layout="horizontal"
          labelCol={{ span: 6 }}
          wrapperCol={{ span: 10 }}
        >
          <Row gutter={[16, 24]}>
            <Col xs={8} sm={8} md={8}>
              <Form.Item name="orderNo" label="订单编号">
                <Input placeholder="请输入订单编号" />
              </Form.Item>
            </Col>
            <Col xs={8} sm={8} md={8}>
              <Form.Item name="taskStatus" label="状态">
                <Select 
                  placeholder="请选择任务状态"
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
          onClick={handleStartTasks}
          disabled={selectedRowKeys.length === 0}
        >
          开始任务
        </Button>
        <Button 
          type="primary" 
          onClick={() => setCreateTimeslotModalVisible(true)}
          disabled={selectedRowKeys.length === 0}
        >
          创建时间槽
        </Button>
      </Space>
      <Table
        rowKey="taskNo"
        columns={columns}
        dataSource={orderTasks}
        rowSelection={{ type: 'checkbox', ...rowSelection }}
        loading={loading}
        scroll={{ x: 1500 }}
        pagination={{ 
          pageSize: 20, 
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条记录` 
        }}
      />
      {/* 创建时间槽模态框 - 基于任务 */}
      <Modal
        title="创建时间槽"
        open={createTimeslotModalVisible}
        onOk={handleCreateTimeslot}
        onCancel={() => {
          setCreateTimeslotModalVisible(false);
          timeslotForm.resetFields();
        }}
        okText="确认"
        cancelText="取消"
        okButtonProps={{ loading: creatingTimeslot }}
      >
        <Form
          form={timeslotForm}
          layout="vertical"
          initialValues={{
            time: 0.5
          }}
        >
          <Form.Item
            name="time"
            label="时间（小时）"
            dependencies={['slice']}
            rules={[
              {
                validator: (_, value, callback) => {
                  const slice = timeslotForm.getFieldValue('slice');
                  if (value !== undefined && value !== null && slice !== undefined && slice !== null && slice !== '') {
                    callback('时间和分片序号只能填写一个');
                  } else if (value === undefined && slice === undefined) {
                    callback('请至少填写时间或分片序号中的一个');
                  } else {
                    callback();
                  }
                }
              }
            ]}
          >
            <InputNumber min={0.1} max={24} step={0.1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="slice"
            label="分片序号"
            dependencies={['time']}
            rules={[
              {
                validator: (_, value, callback) => {
                  const time = timeslotForm.getFieldValue('time');
                  if (value !== undefined && value !== null && value !== '' && time !== undefined && time !== null) {
                    callback('时间和分片序号只能填写一个');
                  } else if (value === undefined && time === undefined) {
                    callback('请至少填写时间或分片序号中的一个');
                  } else {
                    callback();
                  }
                }
              }
            ]}
          >
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
      
      {/* 创建时间槽模态框 - 基于工序 */}
      <Modal
        title="创建时间槽"
        open={createTimeslotByProcedureModalVisible}
        onOk={handleCreateTimeslotByTimeslotIdsConfirm}
        onCancel={() => {
          setCreateTimeslotByProcedureModalVisible(false);
          timeslotForm.resetFields();
        }}
        okText="确认"
        cancelText="取消"
        okButtonProps={{ loading: creatingTimeslot }}
      >
        <Form
          form={timeslotForm}
          layout="vertical"
          initialValues={{
            time: 0.5
          }}
        >
          <Form.Item
            name="time"
            label="时间（小时）"
            dependencies={['slice']}
            rules={[
              {
                validator: (_, value, callback) => {
                  const slice = timeslotForm.getFieldValue('slice');
                  if (value !== undefined && value !== null && slice !== undefined && slice !== null && slice !== '') {
                    callback('时间和分片序号只能填写一个');
                  } else if (value === undefined && slice === undefined) {
                    callback('请至少填写时间或分片序号中的一个');
                  } else {
                    callback();
                  }
                }
              }
            ]}
          >
            <InputNumber min={0.1} max={24} step={0.1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="slice"
            label="分片序号"
            dependencies={['time']}
            rules={[
              {
                validator: (_, value, callback) => {
                  const time = timeslotForm.getFieldValue('time');
                  if (value !== undefined && value !== null && value !== '' && time !== undefined && time !== null) {
                    callback('时间和分片序号只能填写一个');
                  } else if (value === undefined && time === undefined) {
                    callback('请至少填写时间或分片序号中的一个');
                  } else {
                    callback();
                  }
                }
              }
            ]}
          >
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
      
      {/* 时间槽列表模态框 */}
      <Modal
        title={`任务【${currentTaskNo}】时间槽列表`}
        open={timeslotListModalVisible}
        onCancel={() => {
          setTimeslotListModalVisible(false);
          setTimeslotData([]);
          setSelectedTimeslotKeys([]);
        }}
        width={900}
        footer={[
          <Button key="close" onClick={() => {
            setTimeslotListModalVisible(false);
            setTimeslotData([]);
            setSelectedTimeslotKeys([]);
          }}>
            关闭
          </Button>,
        ]}
      >
        {/* 创建时间槽按钮 - 移到表格上方 */}
        <div style={{ marginBottom: 16, textAlign: 'right' }}>
          <Button
              type="primary"
              onClick={handleCreateTimeslotByTimeslotIds}
              disabled={selectedTimeslotKeys.length === 0}
            >
              创建时间槽
            </Button>
        </div>
        <Table
          rowKey="id"
          columns={timeslotColumns}
          dataSource={timeslotData}
          loading={timeslotLoading}
          scroll={{ x: 1200 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`
          }}
          rowSelection={{
            selectedRowKeys: selectedTimeslotKeys,
            onChange: (newSelectedRowKeys) => {
              setSelectedTimeslotKeys(newSelectedRowKeys as string[]);
            },
            // 只有机器时间大于30分钟时才显示选择框
            getCheckboxProps: (record: Timeslot) => ({
              disabled: record.duration <= 30,
            }),
          }}
        />
      </Modal>
    </div>
  );
};

export default OrderTasksPage;
