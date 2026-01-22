import React, {useEffect, useState, useCallback} from 'react';
import {Card, InputNumber, message, Modal, Spin, Table, Tag, Tooltip, Form, Input, DatePicker, Row, Col, Button, Space} from 'antd';
import type {ColumnType} from 'antd/es/table';
import {getTimeslotList, splitOutsourcingTimeslot, queryTimeslots} from '../services/api';
import type {Procedure, Timeslot} from '../services/model';
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
  [dateKey: string]: string | Timeslot[] | undefined;
}

const SchedulingTimelinePage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<TableData[]>([]);
  const [dateColumns, setDateColumns] = useState<string[]>([]);
  // 弹窗状态管理
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [currentTimeslotId, setCurrentTimeslotId] = useState("");
  const [days, setDays] = useState(0);
  // 分页状态
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  // 筛选状态
  const [filterVisible, setFilterVisible] = useState(false);

  // 格式化工序详情
  const formatProcedureDetail = (timeslot: Timeslot) => {
    const { procedure, workCenter, startTime, endTime, duration } = timeslot;
    const durationHours = duration ? Number((duration).toFixed(2)) : 0;
    return (
      <div style={{ fontSize: '12px', lineHeight: '1.5' }}>
        <p><strong>工序名称：</strong>{procedure?.procedureName || '未知'}（{procedure?.procedureNo || '未知'}）</p>
        <p><strong>工作中心：</strong>{workCenter?.name || '未知'}</p>
        <p><strong>开始时间：</strong>{startTime || '未知'}</p>
        <p><strong>结束时间：</strong>{endTime || '未知'}</p>
        <p><strong>持续时间：</strong>{durationHours} 分钟</p>
        <p><strong>工序状态：</strong>{procedure?.status || '未知'}</p>
        <p><strong>工序序号：</strong>{procedure?.procedureNo || '未知'}</p>
        {procedure?.parallel && <p><strong>并行工序：</strong>是</p>}
        <p><strong>机器工时：</strong>{procedure?.machineMinutes || 0} 分钟</p>
        <p><strong>人工工时：</strong>{procedure?.humanMinutes || 0} 分钟</p>
      </div>
    );
  };

  // 为工序分配颜色，确保nextProcedureNo中下一道工序的背景颜色相同
  const getProcedureColor = (procedure: Procedure | undefined) => {
    // 只有并行工序显示背景颜色，其他工序使用默认颜色
    if (!procedure || !procedure.parallel) {
      return '#f5f5f5'; // 非并行工序使用默认背景色，不突出显示
    }
    
    // 为不同的并行工序组分配不同的颜色
    const colors = [
      '#e6f7ff', // 浅蓝色
      '#f6ffed', // 浅绿色
      '#fff7e6', // 浅黄色
      '#fff1f0', // 浅红色
      '#f9f0ff', // 浅紫色
      '#e6fffb', // 浅青色
      '#fffbe6', // 浅金色
      '#f0f5ff'  // 浅蓝紫色
    ];
    
    // 使用工序号的十位数字作为颜色分配的依据
    // 这样可以确保同一组的并行工序（如50和60，它们的十位都是5）有相同的颜色
    const tensDigit = Math.floor(procedure.procedureNo / 10);
    const index = tensDigit % colors.length;
    return colors[index];
  };

  // 显示拆分弹窗
  const showSplitModal = (timeslotId: string) => {
    setCurrentTimeslotId(timeslotId);
    setDays(0); // 重置天数为0
    setIsModalVisible(true);
  };

  // 渲染单元格内容
  const renderCellContent = (timeslots?: Timeslot[], currentDate?: string) => {
    if (!timeslots || timeslots.length === 0) {
      return <div style={{ textAlign: 'center', padding: '8px', color: '#999' }}>未安排</div>;
    }
    
    // 显示时间槽在当前日期的时间段
    const getDisplayTimeRange = (timeslot: Timeslot, date: string) => {
      if (!timeslot.startTime || !timeslot.endTime) return '';
      
      const startTime = timeslot.startTime;
      const endTime = timeslot.endTime;
      const startDate = startTime.substring(0, 10);
      const endDate = endTime.substring(0, 10);
      
      if (startDate === date && endDate === date) {
        // 同一日期内的时间槽
        return `${startTime.substring(11, 16)} - ${endTime.substring(11, 16)}`;
      } else if (startDate === date) {
        // 开始日期
        return `${startTime.substring(11, 16)} - 24:00`;
      } else if (endDate === date) {
        // 结束日期
        return `00:00 - ${endTime.substring(11, 16)}`;
      } else {
        // 中间日期
        return '00:00 - 24:00';
      }
    };
    
    return (
      <div style={{ fontSize: '11px', display: 'flex', flexDirection: 'column', gap: '2px' }}>
        {timeslots.map((ts) => {
          const baseBgColor = getProcedureColor(ts.procedure);
          return (
            <Tooltip 
              key={ts.id}
              title={formatProcedureDetail(ts)}
              placement="topLeft"
              mouseEnterDelay={1} // 1秒后显示
              overlayStyle={{ 
                maxWidth: '300px', 
                padding: '10px',
                borderRadius: '6px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
              }}
            >
              <div 
                style={{ 
                  padding: '4px', 
                  background: baseBgColor, 
                  borderRadius: '4px',
                  border: '1px solid #e8e8e8',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease'
                }}
                onMouseOver={(e) => {
                  // 添加悬停效果
                  e.currentTarget.style.background = '#e6f7ff';
                  e.currentTarget.style.borderColor = '#91d5ff';
                }}
                onMouseOut={(e) => {
                  // 移除悬停效果
                  e.currentTarget.style.background = baseBgColor;
                  e.currentTarget.style.borderColor = '#e8e8e8';
                }}
              >
                <div 
                  style={{ 
                    fontWeight: 'bold', 
                    color: '#333', 
                    marginBottom: '2px',
                    cursor: ts.workCenter?.workCenterCode === "PM10W200" ? 'pointer' : 'default'
                  }}
                  onClick={() => {
                    if (ts.workCenter?.workCenterCode === "PM10W200") {
                      showSplitModal(ts.id);
                    }
                  }}
                >
                  {ts.procedure?.procedureName || '未知'}（{ts.procedure?.procedureNo || '未知'}）
                </div>
                {currentDate && (
                  <div style={{ color: '#666', fontSize: '10px' }}>
                    {getDisplayTimeRange(ts, currentDate)}
                  </div>
                )}
                <div style={{ marginTop: '2px' }}>
                  <Tag 
                    color={
                      ts.procedure?.status === '执行中' ? 'blue' :
                      ts.procedure?.status === '执行完成' ? 'green' :
                      ts.procedure?.status === '待执行' ? 'orange' : 'yellow'
                    }>
                    {ts.procedure?.status || '未知'}
                  </Tag>
                </div>
              </div>
            </Tooltip>
          );
        })}
      </div>
    );
  };

  // 格式化日期显示
  const formatDate = (dateStr: string) => {
    // 将YYYY-MM-DD格式转换为更友好的显示
    return moment(dateStr).format('M月D日');
  };

  // 渲染时间轴
  const renderTimeline = (record: TableData) => {
    // 从record中获取任务号
    const taskNo = record.taskNo;
    // 从所有任务时间槽中获取该任务的时间槽
    let taskTimeslots: Timeslot[] = [];
    
    // 遍历所有可能的日期列，收集该任务的时间槽
    dateColumns.forEach(date => {
      const timeslots = record[date] as Timeslot[];
      if (timeslots) {
        taskTimeslots = [...taskTimeslots, ...timeslots];
      }
    });
    
    // 去重，避免同一时间槽被多次添加
    const uniqueTimeslots = Array.from(new Set(taskTimeslots.map(ts => ts.id)))
      .map(id => taskTimeslots.find(ts => ts.id === id)!) || [];
    
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
    
    // 渲染时间块的位置和宽度
    const getPositionAndWidth = (ts: Timeslot) => {
      if (!ts.startTime) return { left: '0%', width: '100%' };
      
      const startTime = new Date(ts.startTime);
      const endTime = ts.endTime ? new Date(ts.endTime) : new Date(startTime.getTime() + 60 * 60 * 1000); // 默认1小时
      
      const left = ((startTime.getTime() - minTime!.getTime()) / timeRange) * 100;
      const width = ((endTime.getTime() - startTime.getTime()) / timeRange) * 100;
      
      return { 
        left: `${Math.max(0, left)}%`, 
        width: `${Math.max(1, width)}%` 
      };
    };
    
    return (
      <div style={{ height: '150px', overflow: 'auto', border: '1px solid #f0f0f0', borderRadius: '4px', padding: '12px' }}>
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
            <div style={{ position: 'relative', height: '80px' }}>
              {/* 时间轴线 */}
              <div style={{ 
                position: 'absolute', 
                left: '80px', 
                right: '0', 
                top: '50%', 
                height: '2px', 
                background: '#f0f0f0',
                transform: 'translateY(-50%)'
              }} />
              
              {/* 时间块 */}
              {sortedTimeslots.map((ts, index) => {
                const { left, width } = getPositionAndWidth(ts);
                return (
                  <div key={ts.id} style={{ 
                    position: 'absolute', 
                    left: `calc(80px + ${left})`, 
                    width: width,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    zIndex: 1
                  }}>
                    {/* 时间块主体 */}
                    <div style={{
                      width: '100%',
                      height: '40px',
                      padding: '8px',
                      border: '1px solid #f0f0f0',
                      borderRadius: '4px',
                      background: ts.procedure?.status === '执行中' ? '#e6f7ff' : 
                                  ts.procedure?.status === '执行完成' ? '#f6ffed' : 
                                  ts.procedure?.status === '待执行' ? '#fff7e6' : 'white',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                      fontSize: '10px',
                      textAlign: 'center',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis'
                    }}>
                      <div style={{ fontWeight: 'bold', marginBottom: '2px' }}>
                        {ts.procedure?.procedureName || '未知'}
                      </div>
                      <div style={{ fontSize: '9px', color: '#999' }}>
                        {ts.procedure?.procedureNo || ''}
                      </div>
                    </div>
                    
                    {/* 连接线 */}
                    <div style={{
                      width: '1px',
                      height: '10px',
                      background: '#f0f0f0',
                      marginTop: '2px'
                    }} />
                    
                    {/* 时间点 */}
                    <div style={{
                      width: '8px',
                      height: '8px',
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
            </div>
            
            {/* 工序列表 */}
            <div style={{ marginTop: '12px', fontSize: '11px', color: '#666' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>工序列表：</div>
              <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                {sortedTimeslots.map((ts, index) => (
                  <div key={ts.id} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <div style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '50%',
                      background: ts.procedure?.status === '执行中' ? '#1890ff' : 
                                  ts.procedure?.status === '执行完成' ? '#52c41a' : 
                                  ts.procedure?.status === '待执行' ? '#faad14' : '#d9d9d9'
                    }} />
                    <span>{index + 1}. {ts.procedure?.procedureName || '未知'}</span>
                    <span style={{ color: '#999' }}>
                      ({ts.startTime ? moment(ts.startTime).format('YYYY-MM-DD HH:mm') : '?'}-{ts.endTime ? moment(ts.endTime).format('YYYY-MM-DD HH:mm') : '?'})
                    </span>
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
        taskNo
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
        setDateColumns(dates);
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

  // 处理弹窗确认
  const handleOk = async () => {
    if (days <= 0) {
      message.warning('请输入大于0的天数');
      return;
    }

    try {
      await splitOutsourcingTimeslot(currentTimeslotId, days);
      message.success('拆分成功');
      setIsModalVisible(false);
      // 刷新数据
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
        pageNum: currentPage,
        pageSize
      });
    } catch (error) {
      message.error('拆分失败: ' + (error instanceof Error ? error.message : '未知错误'));
    }
  };

  // 处理弹窗取消
  const handleCancel = () => {
    setIsModalVisible(false);
  };

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
      <Modal
        title="输入预计完成天数"
        open={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
        okText="确认"
        cancelText="取消"
      >
        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>预计完成天数：</label>
          <InputNumber
            min={1}
            value={days}
            onChange={(value) => setDays(value || 0)}
            style={{ width: '100%' }}
            placeholder="请输入天数"
          />
        </div>
      </Modal>
    </div>
  );
};

export default SchedulingTimelinePage;
