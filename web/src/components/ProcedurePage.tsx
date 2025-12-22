import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Space, Typography, message, Form, Input, Select, Row, Col, Card, Layout, List, Pagination } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { queryProceduresWithPagination } from '../services/api.ts';
import type { Procedure, ProcedureQueryParams } from '../services/model.ts';
import moment from 'moment';


const { Title } = Typography;
const { Option } = Select;
const { Sider, Content } = Layout;

interface GroupedProcedure {
  key: string;
  taskNo: string;
  orderNo: string;
  procedures: Procedure[];
  // 时间相关属性
  factStartDate: string | null;
  factEndDate: string | null;
  planStartDate: string | null;
  planEndDate: string | null;
}

const ProcedurePage: React.FC = () => {
  const [form] = Form.useForm();
  const [groupedProcedures, setGroupedProcedures] = useState<GroupedProcedure[]>([]);
  const [selectedTask, setSelectedTask] = useState<GroupedProcedure | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0
  });
  
  // 工序状态选项
  const statusOptions = [
    { label: '待生产', value: '待生产' },
    { label: '生产中', value: '生产中' },
    { label: '生产完成', value: '生产完成' },
    { label: '已暂停', value: '已暂停' },
    { label: '初始导入', value: '初始导入' },
    { label: '待执行', value: '待执行' },
  ];
  
  // 查询工序列表数据
  const fetchProcedures = useCallback(async (params: ProcedureQueryParams) => {
    setLoading(true);
    try {
      const response = await queryProceduresWithPagination(params);
      
      // 根据实际API返回结构，TaskWithProcedures数组在response.data.content中
      const taskWithProceduresList = response.data?.content || [];
      
      // 将TaskWithProcedures转换为GroupedProcedure格式
      const groupedProcedures = taskWithProceduresList.map(task => {
        // 对任务的工序按工序编号从小到大排序
        const sortedProcedures = (task.procedures || []).sort((a, b) => {
          // 确保procedureNo是数字类型进行比较
          const noA = a.procedureNo;
          const noB = b.procedureNo;
          return noA - noB;
        });
        
        return {
          key: task.taskNo,
          taskNo: task.taskNo,
          orderNo: task.orderNo,
          procedures: sortedProcedures,
          // 直接使用任务级别的时间字段
          factStartDate: task.factStartDate,
          factEndDate: task.factEndDate,
          planStartDate: task.planStartDate,
          planEndDate: task.planEndDate
        };
      });
      
      setGroupedProcedures(groupedProcedures);
      
      // 根据API返回结构设置分页信息
      const pageData = response.data;
      setPagination({
        current: pageData.number + 1, // Spring Data的页码从0开始，转换为前端从1开始
        pageSize: pageData.size,
        total: pageData.totalElements
      });
    } catch (error: unknown) {
      message.error('网络错误，获取工序数据失败: ' + (error instanceof Error ? error.message : '未知错误'));
      setGroupedProcedures([]);
      setPagination({
        current: 1,
        pageSize: 20,
        total: 0
      });
    } finally {
      setLoading(false);
    }
  }, [setGroupedProcedures, setPagination, setLoading]);
  
  // 初始加载数据
  useEffect(() => {
    const params: ProcedureQueryParams = {
      ...form.getFieldsValue(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    };
    fetchProcedures(params);
  }, []);
  
  // 处理搜索
  const handleSearch = async () => {
    const values = form.getFieldsValue();
    
    const params: ProcedureQueryParams = {
      ...values,
      pageNum: 1,
      pageSize: pagination.pageSize
    };
    
    fetchProcedures(params);
  };
  
  // 处理重置
  const handleReset = () => {
    form.resetFields();
    // 重新查询
    fetchProcedures({
      pageNum: 1,
      pageSize: pagination.pageSize
    });
  };
  
  // 处理分页变化
  const handlePaginationChange = (page: number, pageSize: number) => {
    const params: ProcedureQueryParams = {
      ...form.getFieldsValue(),
      pageNum: page,
      pageSize: pageSize
    };
    setPagination({
      ...pagination,
      current: page,
      pageSize: pageSize
    });
    fetchProcedures(params);
  };
  


  // 子表格列定义（工序详情）
  const detailColumns: ColumnsType<Procedure> = [
    {
      title: '工序名称',
      dataIndex: 'procedureName',
      key: 'procedureName',
    },
    {
      title: '工序状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = '';
        switch (status) {
          case '待执行':
            color = 'blue';
            break;
          case '执行中':
            color = 'green';
            break;
          case '生产完成':
            color = 'orange';
            break;
          case '已暂停':
            color = 'red';
            break;
          default:
            color = 'default';
        }
        return <span style={{ color }}>{status}</span>;
      }
    },
    {
      title: '工序编号',
      dataIndex: 'procedureNo',
      key: 'procedureNo',
    },
    {
      title: '下一道工序',
      dataIndex: 'nextProcedureNo',
      key: 'nextProcedureNo',
      render: (nextProcedureNo: number[]) => {
        if (!nextProcedureNo || nextProcedureNo.length === 0) {
          return '-';
        }
        return nextProcedureNo.join(', ');
      },
    },
    {
      title: '工作中心',
      dataIndex: ['workCenterId', 'name'],
      key: 'workCenterName',
    },
    {
      title: '机器时间(分钟)',
      dataIndex: 'machineMinutes',
      key: 'machineMinutes',
    },
    { title: '实际开始时间',
      dataIndex: 'factStartDate',
      key: 'factStartDate',
      render: (text: string) => {
        if (!text) return '';
        try {
          return moment(text).format('YYYY-DD-MM');
        } catch {
          return text;
        }
      },
    },
    { title: '实际结束时间',
      dataIndex: 'factEndDate',
      key: 'factEndDate',
      render: (text: string) => {
        if (!text) return '';
        try {
          return moment(text).format('YYYY-DD-MM');
        } catch {
          return text;
        }
      },
    },
    { title: '计划开始日期',
      dataIndex: 'planStartDate',
      key: 'planStartDate',
      render: (text: string | null) => {
        if (!text) return '';
        try {
          return moment(text).format('YYYY-DD-MM');
        } catch {
          return text;
        }
      },
    },
    { title: '计划结束日期',
      dataIndex: 'planEndDate',
      key: 'planEndDate',
      render: (text: string | null) => {
        if (!text) return '';
        try {
          return moment(text).format('YYYY-DD-MM');
        } catch {
          return text;
        }
      },
    },
    {
      title: '是否并行',
      dataIndex: 'parallel',
      key: 'parallel',
      render: (parallel: boolean) => parallel ? '是' : '否',
    },
  ];
  return (
    <div style={{ padding: 8 }}>
      <Title level={4}>工序列表</Title>
      <Card title="查询条件" style={{ marginBottom: 16, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)', padding: '10px 16px' }}>
        <Form
          form={form}
          layout="horizontal"
          labelCol={{ span: 6 }}
          wrapperCol={{ span: 18 }}
          size="small"
          style={{ marginBottom: 0 }}
        >
          <Row gutter={[12, 8]} align="middle">
            <Col xs={24} sm={12} md={6} lg={5}>
              <Form.Item name="orderNo" label="订单编号" style={{ marginBottom: 0 }}>
                <Input placeholder="请输入订单编号" size="small" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={6} lg={5}>
              <Form.Item name="taskNo" label="任务编号" style={{ marginBottom: 0 }}>
                <Input placeholder="请输入任务编号" size="small" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={6} lg={5}>
              <Form.Item name="status" label="工序状态" style={{ marginBottom: 0 }}>
                <Select 
                  placeholder="请选择工序状态"
                  allowClear
                  style={{ width: '100%' }}
                  size="small"
                >
                  {statusOptions.map(option => (
                    <Option key={option.value} value={option.value}>{option.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={6} lg={9}>
              <Space size="small" style={{ paddingLeft: 2 }}>
                <Button type="primary" onClick={handleSearch} size="small">
                  查询
                </Button>
                <Button onClick={handleReset} size="small">
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>
      </Card>
      
      <Layout style={{ height: 600, border: '1px solid #f0f0f0' }}>
        <Sider width={210} style={{ background: '#fafafa', overflow: 'auto' }}>
          <Card title="任务列表" style={{ height: '100%', border: 'none', display: 'flex', flexDirection: 'column' }}>
            <List
              dataSource={groupedProcedures}
              renderItem={(item) => (
                <List.Item
                  key={item.key}
                  onClick={() => setSelectedTask(item)}
                  style={{
                    cursor: 'pointer',
                    borderLeft: selectedTask?.key === item.key ? '1px solid #1890ff' : 'none',
                    backgroundColor: selectedTask?.key === item.key ? '#e6f7ff' : 'transparent',
                    paddingLeft: 12
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 'bold' }}>{item.taskNo}</div>
                    {(() => {
                      // 显示任务级别的时间信息
                      if (!item.procedures || item.procedures.length === 0) {
                        return (
                          <div style={{ fontSize: 12, color: '#999' }}>
                            无工序信息
                          </div>
                        );
                      }

                      // 检查是否有实际开始和结束时间
                      const hasActualFactStart = item.factStartDate;
                      const hasActualFactEnd = item.factEndDate;
                      const hasPlanStart = item.planStartDate;
                      const hasPlanEnd = item.planEndDate;

                      // 格式化时间显示为YYYY-DD-MM格式
                      const formatTime = (timeString: string | null) => {
                        if (!timeString) return '--';
                        try {
                          return moment(timeString).format('MM-DD');
                        } catch {
                          return timeString;
                        }
                      };

                      // 处理不同的时间组合情况
                      if (hasActualFactStart && hasActualFactEnd) {
                        // 有实际开始和结束时间
                        return (
                          <div style={{ fontSize: 12, color: '#666' }}>
                            {formatTime(item.factStartDate)} - {formatTime(item.factEndDate)}
                          </div>
                        );
                      } else if (hasPlanStart && hasPlanEnd) {
                        // 有计划开始和结束时间
                        return (
                          <div style={{ fontSize: 12, color: 'gray' }}>
                            {formatTime(item.planStartDate)} 至 {formatTime(item.planEndDate)}
                          </div>
                        );
                      } else {
                        // 只有部分时间或没有时间
                        return (
                          <div style={{ fontSize: 12, color: '#999' }}>
                            时间信息不完整
                          </div>
                        );
                      }
                    })()}
                  </div>
                </List.Item>
              )}
              style={{ padding: 0 }}
              loading={loading}
            />
            <div style={{ padding: '1px 0' }}>
                <Pagination
                  {...pagination}
                  showSizeChanger
                  showTotal={() => null}
                  onChange={handlePaginationChange}
                  onShowSizeChange={handlePaginationChange}
                  size="small"
                />
              </div>
          </Card>
        </Sider>
        <Content style={{ padding: '0 1px', overflow: 'auto' }}>
          <Card title="工序详情" style={{ height: '100%', border: 'none', display: 'flex', flexDirection: 'column' }}>
            {selectedTask ? (
              <Table
                columns={detailColumns}
                dataSource={selectedTask.procedures}
                rowKey="id"
                pagination={false}
                scroll={{ x: 2000, y: 'calc(100vh - 400px)' }}
                style={{ flex: 1 }}
              />
            ) : (
              <div style={{ textAlign: 'center', padding: 100, color: '#999', flex: 1 }}>
                请选择一个任务查看工序详情
              </div>
            )}
          </Card>
        </Content>
      </Layout>
    </div>
  );
};

export default ProcedurePage;
