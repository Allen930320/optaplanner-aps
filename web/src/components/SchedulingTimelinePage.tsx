import React, {useEffect, useState, useCallback} from 'react';
import {Card, message, Spin, Table, Form, Input, DatePicker, Row, Col, Button, Space} from 'antd';
import type {ColumnType} from 'antd/es/table';
import {queryTimeslots} from '../services/api';
import type {Timeslot} from '../services/model';
import moment from 'moment';
import {SearchOutlined, FilterOutlined} from '@ant-design/icons';

interface TaskTimeslot {
  orderNo: string;
  taskNo: string;
  contractNum: string;
  productCode: string;
  productName: string;
  timeslots: Timeslot[];
}

interface TaskData {
  [key: string]: { timeslots: Timeslot[]; dateMap: Map<string, Timeslot[]> };
}

interface TableData {
  key: string;
  taskNo: string;
  allTimeslots?: Timeslot[];
  [dateKey: string]: string | Timeslot[] | undefined;
}

const SchedulingTimelinePage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<TableData[]>([]);

  // 分页状态
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  // 筛选状态
  const [filterVisible, setFilterVisible] = useState(false);







  // 渲染时间轴
  const renderTimeline = (record: TableData) => {
    // 从allTimeslots中获取该任务的所有时间槽
    const uniqueTimeslots = record.allTimeslots || [];

    // 按开始时间排序时间槽
    const sortedTimeslots = [...uniqueTimeslots].sort((a, b) => {
      if (!a.startTime) return 1;
      if (!b.startTime) return -1;
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
    });

    // 计算时间轴范围
    let minTime: Date | null = null;
    let maxTime: Date | null = null;

    sortedTimeslots.forEach(ts => {
      if (ts.startTime) {
        const startTime = new Date(ts.startTime);
        minTime = minTime ? (startTime < minTime ? startTime : minTime) : startTime;
      }
      if (ts.endTime) {
        const endTime = new Date(ts.endTime);
        maxTime = maxTime ? (endTime > maxTime ? endTime : maxTime) : endTime;
      }
    });

    // 如果没有时间信息，使用当前时间作为默认值
    if (!minTime) minTime = new Date();
    if (!maxTime) maxTime = new Date(minTime.getTime() + 2 * 60 * 60 * 1000); // 默认2小时

    // 计算时间轴总长度（毫秒）
    const timeRange = maxTime.getTime() - minTime.getTime();

    // 为工序分配颜色
    const getProcedureColor = (timeslot: Timeslot) => {
      const procedure = timeslot.procedure;

      // 定义颜色数组
      const colors = [
        '#1890ff', // 蓝色
        '#52c41a', // 绿色
        '#faad14', // 黄色
        '#f5222d', // 红色
        '#722ed1', // 紫色
        '#13c2c2', // 青色
        '#fa541c', // 橙色
        '#eb2f96', // 粉色
        '#a0d911', // 浅绿色
        '#fa8c16'  // 深橙色
      ];

      // 使用工序号、名称或时间槽ID生成哈希值，确保每个工序都有唯一颜色
      let key = 'default';
      if (procedure) {
        key = String(procedure.procedureNo || procedure.procedureName || 'default');
      }
      // 如果仍然是default，使用时间槽ID确保唯一性
      if (key === 'default' && timeslot.id) {
        key = timeslot.id;
      }

      let hash = 0;
      for (let i = 0; i < key.length; i++) {
        hash = key.charCodeAt(i) + ((hash << 5) - hash);
      }

      // 将哈希值映射到颜色数组索引
      const index = Math.abs(hash) % colors.length;
      return colors[index];
    };

    // 渲染时间块的位置和宽度
    const getPositionAndWidth = (ts: Timeslot) => {
      if (!ts.startTime) return { left: '0%', width: '100%' };

      const startTime = new Date(ts.startTime);
      const endTime = ts.endTime ? new Date(ts.endTime) : new Date(startTime.getTime() + 60 * 60 * 1000); // 默认1小时

      const left = ((startTime.getTime() - minTime!.getTime()) / timeRange) * 100;
      const width = ((endTime.getTime() - startTime.getTime()) / timeRange) * 100;

      // 确保时间块不会超出时间轴范围
      const adjustedLeft = Math.max(0, left);
      const adjustedWidth = Math.min(100 - adjustedLeft, Math.max(1, width));

      return {
        left: `${adjustedLeft}%`,
        width: `${adjustedWidth}%`
      };
    };

    // 检测两个时间槽是否重叠
    const isOverlapping = (ts1: Timeslot, ts2: Timeslot) => {
      if (!ts1.startTime || !ts2.startTime) return false;

      const start1 = new Date(ts1.startTime).getTime();
      const end1 = ts1.endTime ? new Date(ts1.endTime).getTime() : start1 + 60 * 60 * 1000;
      const start2 = new Date(ts2.startTime).getTime();
      const end2 = ts2.endTime ? new Date(ts2.endTime).getTime() : start2 + 60 * 60 * 1000;

      return start1 < end2 && start2 < end1;
    };

    // 为时间槽分配轨道（行），确保同一轨道上的时间槽不重叠
    const assignTracks = (timeslots: Timeslot[]) => {
      const tracks: Timeslot[][] = [];

      timeslots.forEach(ts => {
        let assigned = false;

        // 尝试将时间槽分配到现有轨道
        for (const track of tracks) {
          // 检查轨道上的所有时间槽是否与当前时间槽重叠
          const hasOverlap = track.some(existingTs => isOverlapping(existingTs, ts));
          if (!hasOverlap) {
            // 没有重叠，分配到该轨道
            track.push(ts);
            assigned = true;
            break;
          }
        }

        // 如果没有找到合适的轨道，创建新轨道
        if (!assigned) {
          tracks.push([ts]);
        }
      });

      return tracks;
    };

    // 分配轨道
    const tracks = assignTracks(sortedTimeslots);
    const trackCount = tracks.length;
    const trackHeight = 50; // 每个轨道的高度
    const totalHeight = Math.max(80, trackCount * trackHeight); // 总高度，至少80px

    return (
        <div style={{ height: 'auto', minHeight: '150px', maxHeight: '300px', overflow: 'auto', border: '1px solid #f0f0f0', borderRadius: '4px', padding: '12px' }}>
          {sortedTimeslots.length > 0 ? (
              <div>
                {/* 时间轴刻度 */}
                <div style={{ height: '20px', marginBottom: '12px', display: 'flex', alignItems: 'center', fontSize: '10px', color: '#999' }}>
                  <div style={{ width: '80px', flexShrink: 0 }}>时间</div>
                  <div style={{ flex: 1, position: 'relative' }}>
                    {/* 时间点 */}
                    <div style={{ position: 'absolute', left: '0%', top: '0' }}>
                      {moment(minTime).format('YYYY-MM-DD HH:mm')}
                    </div>
                    <div style={{ position: 'absolute', left: '50%', top: '0' }}>
                      {moment(new Date(minTime.getTime() + timeRange / 2)).format('YYYY-MM-DD HH:mm')}
                    </div>
                    <div style={{ position: 'absolute', left: '100%', top: '0', transform: 'translateX(-100%)' }}>
                      {moment(maxTime).format('YYYY-MM-DD HH:mm')}
                    </div>
                  </div>
                </div>

                {/* 时间轴和时间块 */}
                <div style={{ display: 'flex', height: totalHeight }}>
                  <div style={{ width: '80px', flexShrink: 0 }}></div>
                  <div style={{ flex: 1, position: 'relative' }}>
                    {/* 时间轴线（每个轨道一条） */}
                    {tracks.map((_, trackIndex) => (
                        <div key={trackIndex} style={{
                          position: 'absolute',
                          left: '0',
                          right: '-20px', // 向右延伸20px，与延长线对齐
                          top: `${trackIndex * trackHeight + trackHeight / 2}px`,
                          height: '2px',
                          background: '#f0f0f0',
                          transform: 'translateY(-50%)'
                        }} />
                    ))}

                    {/* 时间块 */}
                    {tracks.map((track, trackIndex) => (
                        <>
                          {track.map((ts) => {
                            const { left, width } = getPositionAndWidth(ts);
                            return (
                                <div key={ts.id} style={{
                                  position: 'absolute',
                                  left: left,
                                  width: width,
                                  top: `${trackIndex * trackHeight + trackHeight / 2}px`,
                                  transform: 'translateY(-50%)',
                                  display: 'flex',
                                  flexDirection: 'column',
                                  alignItems: 'center',
                                  zIndex: 1
                                }}>
                                  {/* 时间块主体 */}
                                  <div style={{
                                    width: '100%',
                                    height: '30px',
                                    border: '1px solid #f0f0f0',
                                    borderRadius: '4px',
                                    background: getProcedureColor(ts),
                                    boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                                    overflow: 'hidden'
                                  }} />
                                  {/* 工序名称（可选，悬停时显示） */}
                                  <div style={{
                                    position: 'absolute',
                                    bottom: '100%',
                                    left: '50%',
                                    transform: 'translateX(-50%)',
                                    backgroundColor: 'rgba(0,0,0,0.8)',
                                    color: 'white',
                                    padding: '4px 8px',
                                    borderRadius: '4px',
                                    fontSize: '8px',
                                    whiteSpace: 'nowrap',
                                    zIndex: 10,
                                    opacity: 0,
                                    transition: 'opacity 0.3s ease',
                                    pointerEvents: 'none'
                                  }}>
                                    {ts.procedure?.procedureName || '未知'}({ts.procedure?.procedureNo || ''})
                                  </div>

                                  {/* 连接线 */}
                                  <div style={{
                                    width: '1px',
                                    height: '8px',
                                    background: '#f0f0f0',
                                    marginTop: '2px'
                                  }} />

                                  {/* 时间点 */}
                                  <div style={{
                                    width: '6px',
                                    height: '6px',
                                    borderRadius: '50%',
                                    background: ts.procedure?.status === '执行中' ? '#1890ff' :
                                        ts.procedure?.status === '执行完成' ? '#52c41a' :
                                            ts.procedure?.status === '待执行' ? '#faad14' : '#d9d9d9',
                                    border: '2px solid white',
                                    boxShadow: '0 0 0 1px #f0f0f0',
                                    marginTop: '2px'
                                  }} />
                                </div>
                            );
                          })}

                        </>
                    ))}
                  </div>
                </div>

                {/* 工序列表 */}
                <div style={{ marginTop: '12px', fontSize: '11px', color: '#666' }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>工序列表：</div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {sortedTimeslots.map((ts, index) => (
                        <div key={ts.id} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <div style={{
                              width: '8px',
                              height: '8px',
                              borderRadius: '50%',
                              background: getProcedureColor(ts)
                            }} />
                            <span>{index + 1}. {ts.procedure?.procedureName || '未知'}</span>
                            <span style={{ color: '#999' }}>
                        ({ts.startTime ? moment(ts.startTime).format('YYYY-MM-DD HH:mm') : '?'}-{ts.endTime ? moment(ts.endTime).format('YYYY-MM-DD HH:mm') : '?'})
                      </span>
                          </div>
                          {/* 工序时间轴 */}
                          <div style={{ marginLeft: '12px', width: '100%', height: '10px', position: 'relative' }}>
                            {/* 时间轴背景 */}
                            <div style={{
                              width: '100%',
                              height: '2px',
                              background: '#f0f0f0',
                              position: 'absolute',
                              top: '50%',
                              transform: 'translateY(-50%)'
                            }} />
                            {/* 工序时间块 */}
                            {ts.startTime && (
                                <div style={{
                                  position: 'absolute',
                                  left: getPositionAndWidth(ts).left,
                                  width: getPositionAndWidth(ts).width,
                                  height: '6px',
                                  background: getProcedureColor(ts),
                                  border: '1px solid #f0f0f0',
                                  borderRadius: '3px',
                                  top: '50%',
                                  transform: 'translateY(-50%)'
                                }} />
                            )}
                          </div>
                        </div>
                    ))}
                  </div>
                </div>
              </div>
          ) : (
              <div style={{ textAlign: 'center', color: '#999', padding: '40px 20px' }}>
                暂无时序数据
              </div>
          )}
        </div>
    );
  };

  // 动态生成表格列
  const generateColumns = () => {
    const columns: ColumnType<TableData>[] = [
      {
        title: '任务号',
        dataIndex: 'taskNo',
        key: 'taskNo',
        width: 200,
        fixed: 'left' as const,
        align: 'center' as const,
        ellipsis: true,
        render: (text: string) => (
            <div style={{ fontWeight: 'bold', color: '#1890ff' }}>{text}</div>
        )
      },
      {
        title: '时序图',
        key: 'timeline',
        width: 500,
        fixed: 'left' as const,
        align: 'center' as const,
        render: (_, record) => renderTimeline(record)
      },
    ];

    return columns;
  };

  // 获取时间槽跨越的所有日期
  const getDatesInRange = useCallback((startTime: string, endTime: string): string[] => {
    const startDate = moment(startTime.substring(0, 10));
    const endDate = moment(endTime.substring(0, 10));
    const dates: string[] = [];

    const currentDate = moment(startDate);
    while (currentDate.isSameOrBefore(endDate, 'day')) {
      dates.push(currentDate.format('YYYY-MM-DD'));
      currentDate.add(1, 'day');
    }

    return dates;
  }, []);

  // 根据任务号分组数据
  const groupTimeslotsByTask = useCallback((taskTimeslots: TaskTimeslot[]): TaskData => {
    return taskTimeslots.reduce((acc, taskTimeslot) => {
      const key = taskTimeslot.taskNo;
      if (!acc[key]) {
        acc[key] = { timeslots: [], dateMap: new Map() };
      }

      // 处理每个时间槽
      taskTimeslot.timeslots.forEach(timeslot => {
        acc[key].timeslots.push(timeslot);

        // 按日期分组时间槽
        if (timeslot.startTime && timeslot.endTime) {
          const dates = getDatesInRange(timeslot.startTime, timeslot.endTime);
          dates.forEach(date => {
            if (!acc[key].dateMap.has(date)) {
              acc[key].dateMap.set(date, []);
            }
            acc[key].dateMap.get(date)?.push(timeslot);
          });
        } else {
          // 处理没有时间信息的时间槽，添加到一个默认日期或特殊处理
          // 这里我们将其添加到所有日期的映射中，或者创建一个特殊的标记
          // 为了简单起见，我们可以将其添加到当前日期
          const today = moment().format('YYYY-MM-DD');
          if (!acc[key].dateMap.has(today)) {
            acc[key].dateMap.set(today, []);
          }
          acc[key].dateMap.get(today)?.push(timeslot);
        }
      });

      return acc;
    }, {} as TaskData);
  }, [getDatesInRange]);

  // 提取所有日期
  const extractDates = (taskTimeslots: TaskTimeslot[]): string[] => {
    const dateSet = new Set<string>();
    const today = moment().format('YYYY-MM-DD');

    taskTimeslots.forEach(taskTimeslot => {
      let hasValidTimeslot = false;

      taskTimeslot.timeslots.forEach(timeslot => {
        if (timeslot.startTime) {
          const startDate = timeslot.startTime.substring(0, 10);
          dateSet.add(startDate);
          hasValidTimeslot = true;
        }
        if (timeslot.endTime) {
          const endDate = timeslot.endTime.substring(0, 10);
          dateSet.add(endDate);
          hasValidTimeslot = true;
        }
      });

      // 如果任务没有有效的时间槽，添加今天的日期作为默认
      if (!hasValidTimeslot) {
        dateSet.add(today);
      }
    });

    // 按日期排序
    return Array.from(dateSet).sort();
  };

  // 构建表格数据
  const buildTableData = (groupedData: TaskData, dates: string[]): TableData[] => {
    return Object.entries(groupedData).map(([taskNo, data]) => {
      const row: TableData = {
        key: taskNo, // 将任务号作为key
        taskNo,
        allTimeslots: data.timeslots
      };

      // 为每个日期添加列数据
      dates.forEach(date => {
        row[date] = data.dateMap.get(date) || [];
      });

      return row;
    });
  };

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

    // 重置到第一页
    setCurrentPage(1);
    await fetchData({
      ...values,
      startTime,
      endTime,
      pageNum: 1,
      pageSize
    });
  };

  // 处理重置
  const handleReset = async () => {
    form.resetFields();
    // 重置到第一页
    setCurrentPage(1);
    await fetchData({
      pageNum: 1,
      pageSize
    });
  };

  // 处理分页
  const handlePaginationChange = async (page: number, size: number) => {
    setCurrentPage(page);
    setPageSize(size);
    const values = form.getFieldsValue();
    // 处理日期范围
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

  // 获取数据并预处理
  const fetchData = async (params?: {
    productName?: string;
    productCode?: string;
    contractNum?: string;
    startTime?: string;
    endTime?: string;
    taskNo?: string;
    pageNum?: number;
    pageSize?: number;
  }) => {
    setLoading(true);
    try {
      const response = await queryTimeslots({
        productName: params?.productName || '',
        productCode: params?.productCode || '',
        contractNum: params?.contractNum || '',
        startTime: params?.startTime || '',
        endTime: params?.endTime || '',
        taskNo: params?.taskNo || '',
        pageNum: params?.pageNum || currentPage,
        pageSize: params?.pageSize || pageSize
      });

      if (response.code === 200 && response.data) {
        const taskTimeslots = response.data.content || [];
        setTotal(response.data.totalElements || 0);
        // 按订单号和任务号分组数据
        const grouped = groupTimeslotsByTask(taskTimeslots);
        // 提取所有日期
        const dates = extractDates(taskTimeslots);
        // 构建表格数据
        const table = buildTableData(grouped, dates);
        setTableData(table);
      } else {
        message.error('网络请求失败');
      }
    } catch (error) {
      message.error('网络请求失败: ' + (error instanceof Error ? error.message : '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // 初始加载数据
    fetchData({
      pageNum: currentPage,
      pageSize
    });
  }, []);



  return (
      <div style={{ padding: '20px', minHeight: '100vh', backgroundColor: '#f0f2f5' }}>
        <h1 style={{ marginBottom: '20px', fontSize: '20px', color: '#262626' }}>生产调度时序表</h1>

        {/* 查询条件 */}
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
                className="scheduling-timeline-table"
                style={{ borderCollapse: 'collapse' }}
            />
          </Card>
          {!loading && tableData.length === 0 && (
              <div style={{ textAlign: 'center', padding: '50px', color: '#999', fontSize: '16px' }}>
                暂无调度数据
              </div>
          )}
        </Spin>

        {/* 外协工序时间槽拆分弹窗 */}

      </div>
  );
};

export default SchedulingTimelinePage;
