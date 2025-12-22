import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Button, Card, Form, InputNumber, List, message, Modal, Select, Spin, Table, Tag, TimePicker} from 'antd';
import type {ColumnType} from 'antd/es/table';
import {CalendarOutlined, LoadingOutlined} from '@ant-design/icons';
import type {WorkCenterDetail, WorkCenterMaintenance} from '../services/model';
import {getWorkCenterList, getWorkCenterMaintenance, updateWorkCenterMaintenance} from '../services/api';
import moment from 'moment';


// 日期验证函数
const parseValidDate = (dateString?: string | null): Date | null => {
  if (!dateString) return null;
  try {
    const momentDate = moment(dateString);
    if (momentDate.isValid()) {
      return momentDate.toDate();
    }
    return null;
  } catch {
    return null;
  }
};


interface TableData {
  key: string;
  weekNumber: number;
  [dateKey: string]: string | number | WorkCenterMaintenance[] | undefined;
}

const WorkCenterCalendarPage: React.FC = () => {
  // 状态管理
  const [workCenters, setWorkCenters] = useState<WorkCenterDetail[]>([]);
  const [selectedWorkCenter, setSelectedWorkCenter] = useState<string>('');
  const [selectedMonth, setSelectedMonth] = useState<Date>(new Date());
  const [maintenanceData, setMaintenanceData] = useState<WorkCenterMaintenance[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [workCenterLoading, setWorkCenterLoading] = useState<boolean>(false);
  // 模态框相关状态
  const [isModalVisible, setIsModalVisible] = useState<boolean>(false);
  const [selectedMaintenance, setSelectedMaintenance] = useState<WorkCenterMaintenance | null>(null);
  const [form] = Form.useForm();
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  
  // 生成月份选项
  const generateMonthOptions = () => {
    const options = [];
    const currentYear = new Date().getFullYear();
    // 生成过去2年到未来1年的月份选项
    for (let year = currentYear - 1; year <= currentYear + 1; year++) {
      for (let month = 0; month < 12; month++) {
        const date = new Date(year, month, 1);
        options.push({
          value: moment(date).format('YYYY-MM-DD'),
          label: moment(date).format('YYYY年MM月')
        });
      }
    }
    return options;
  };
  
  const monthOptions = generateMonthOptions();

  // 获取工作中心列表
  useEffect(() => {
    const fetchWorkCenters = async () => {
      setWorkCenterLoading(true);
      try {
        const data = await getWorkCenterList();
        setWorkCenters(data);
        if (data.length > 0) {
          setSelectedWorkCenter(data[0].workCenterCode);
        }
      } catch {
        message.error('获取工作中心列表失败');
      } finally {
        setWorkCenterLoading(false);
      }
    };

    fetchWorkCenters();
  }, []);

  // 当工作中心或月份变化时，获取维护计划数据
  useEffect(() => {
    if (!selectedWorkCenter) return;

    const fetchMaintenanceData = async () => {
      setLoading(true);
      try {
        const startDate = moment(selectedMonth).startOf('month').format('YYYY-MM-DD');
        const endDate = moment(selectedMonth).endOf('month').format('YYYY-MM-DD');
        const data = await getWorkCenterMaintenance(selectedWorkCenter, startDate, endDate);
        // 确保设置的是数组
        const formattedData = Array.isArray(data) ? data : [];
        
        setMaintenanceData(formattedData);
      } catch {
        // 发生错误时设置为空数组
        setMaintenanceData([]);
      } finally {
        setLoading(false);
      }
    };

    fetchMaintenanceData();
  }, [selectedWorkCenter, selectedMonth]);



  // 处理工作中心选择
  const handleWorkCenterSelect = (workCenterCode: string) => {
    setSelectedWorkCenter(workCenterCode);
  };

  // 处理月份选择
  const handleMonthSelect = (value: string | null) => {
    if (value) {
      const date = parseValidDate(value);
      if (date) {
        setSelectedMonth(date);
      }
    }
  };

  // 处理维护记录点击
  const handleMaintenanceClick = useCallback((item: WorkCenterMaintenance) => {
    // 移除id检查，即使没有id也允许弹出模态框
    // 为没有id的记录生成临时id以便更新操作
    const maintenanceItem = {
      ...item,
      id: item.id || `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
    
    setSelectedMaintenance(maintenanceItem);
    form.setFieldsValue({
      capacity: item.capacity,
      status: item.status,
      startTime: item.startTime ? moment(item.startTime, 'YYYY-MM-DD HH:mm'): undefined,
      endTime: item.endTime ? moment(item.endTime, 'YYYY-MM-DD HH:mm') : undefined
    });
    
    setIsModalVisible(true);
  }, [setSelectedMaintenance, form, setIsModalVisible]);

  // 关闭模态框
  const handleModalCancel = () => {
    setIsModalVisible(false);
    setSelectedMaintenance(null);
    form.resetFields();
  };

  // 处理表单提交
  const handleFormSubmit = async () => {
    try {
      const values = await form.validateFields();
      
      if (!selectedMaintenance) {
        message.error('未选中维护记录');
        return;
      }

      // 准备提交数据
      const submitData = {
        id: selectedMaintenance.id,
        workCenterCode: selectedMaintenance.workCenterCode,
        localDate: selectedMaintenance.localDate,
        capacity: values.capacity,
        status: values.status,
        startTime: values.startTime ? moment(values.startTime).format('YYYY-MM-DD HH:mm') : undefined,
        endTime: values.endTime ? moment(values.endTime).format('YYYY-MM-DD HH:mm') : undefined,
        description: selectedMaintenance.description
      };

      setIsSubmitting(true);
      
      // 检查id是否为临时id
      const isTempId = selectedMaintenance.id.startsWith('temp_');
      
      if (isTempId) {
        // 对于临时id，只更新本地数据，不调用API
        message.warning('该记录为临时记录，无法保存到服务器');
        
        // 更新本地数据
        setMaintenanceData(prevData => {
          // 查找是否存在该记录
          const existingIndex = prevData.findIndex(item => 
            item.workCenterCode === selectedMaintenance.workCenterCode && 
            item.localDate === selectedMaintenance.localDate
          );
          
          if (existingIndex >= 0) {
            // 更新现有记录
            const newData = [...prevData];
            newData[existingIndex] = { ...newData[existingIndex], ...submitData };
            return newData;
          } else {
            // 添加新记录
            return [...prevData, submitData];
          }
        });
        
        // 关闭模态框
        handleModalCancel();
      } else {
        // 调用API更新数据
        const response = await updateWorkCenterMaintenance(submitData);
        
        if (response.code === 200) {
          message.success('维护记录更新成功');
          
          // 更新本地数据
          setMaintenanceData(prevData => 
            prevData.map(item => 
              item.id === selectedMaintenance.id 
                ? { ...item, ...submitData } 
                : item
            )
          );
          
          // 关闭模态框
          handleModalCancel();
        } else {
          message.error(response.msg || '更新失败');
        }
      }
    } catch (error) {
      message.error((error as Error).message || '更新失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 生成日历表格数据
  const calendarData = useMemo(() => {
    if (!selectedWorkCenter) return [];
    
    // 获取当前月份的开始和结束日期
    const monthStart = moment(selectedMonth).startOf('month').toDate();
    const monthEnd = moment(selectedMonth).endOf('month').toDate();
    
    // 获取当月第一天所在周的周一和最后一天所在周的周日
    const firstMonday = moment(monthStart).startOf('isoWeek').toDate();
    const lastSunday = moment(monthEnd).endOf('isoWeek').toDate();
    
    // 获取日期范围内的所有日期
    const allDays: Date[] = [];
    const currentDate = moment(firstMonday);
    const endDate = moment(lastSunday);
    
    while (currentDate.isBefore(endDate) || currentDate.isSame(endDate, 'day')) {
      allDays.push(currentDate.toDate());
      currentDate.add(1, 'day');
    }
    
    // 将维护数据按日期分组
    const maintenanceByDate: { [key: string]: WorkCenterMaintenance[] } = {};
    maintenanceData.forEach(item => {
      const dateKey = item.localDate;
      if (!maintenanceByDate[dateKey]) {
        maintenanceByDate[dateKey] = [];
      }
      maintenanceByDate[dateKey].push(item);
    });
    
    // 将日期按周分组
    const weeks: Date[][] = [];
    let currentWeek: Date[] = [];
    
    allDays.forEach(day => {
      currentWeek.push(day);
      // 如果是周日，或者是最后一天，结束当前周
      if (moment(day).day() === 0 || day === allDays[allDays.length - 1]) {
        weeks.push(currentWeek);
        currentWeek = [];
      }
    });
    
    // 生成表格行数据
      return weeks.map((week, weekIndex) => {
        const row: TableData = {
            key: `week_${weekIndex + 1}`,
            weekNumber: weekIndex + 1
        };

        // 为每周的每一天添加数据
        week.forEach(day => {
            const dateKey = moment(day).format('YYYY-MM-DD');
            row[dateKey] = maintenanceByDate[dateKey] || [];
        });

        return row;
    });
  }, [selectedWorkCenter, selectedMonth, maintenanceData]);
  
  // 生成表格列配置
  const tableColumns = useMemo((): ColumnType<TableData>[] => {
    if (!selectedWorkCenter) return [];
    
    // 星期几的中文名称
    const dayNames = ['一', '二', '三', '四', '五', '六', '日'];
    
    // 获取当前月份的开始日期
    const monthStart = moment(selectedMonth).startOf('month').toDate();
    
    // 获取当月第一天所在周的周一
    const firstMonday = moment(monthStart).startOf('isoWeek').toDate();
    
    // 生成列配置
    const columns: ColumnType<TableData>[] = [
      {
        title: '周次',
        dataIndex: 'weekNumber',
        key: 'weekNumber',
        width: 70,
        align: 'center',
        render: (weekNumber: number) => `第${weekNumber}周`
      }
    ];
    
    // 为周一到周日添加列
    dayNames.forEach((dayName, index) => {
      // 注意：这里不再需要columnKey，直接使用日期字符串作为dataIndex
      
      columns.push({
        title: <div style={{ textAlign: 'center' }}>
          <div>{dayName}</div>
        </div>,
        // 使用占位符dataIndex，实际数据通过计算得到
        dataIndex: `day_${index}`,
        key: `day_${index}`,
        width: 120,
        align: 'center',
        render: (_, record) => {
          // 根据行的weekNumber和列的index计算当前单元格对应的日期
          const weekNumber = record.weekNumber as number;
          const currentDate = moment(firstMonday).add((weekNumber - 1) * 7 + index, 'days').toDate();
          const dateKey = moment(currentDate).format('YYYY-MM-DD');
          
          // 获取当前日期的维护计划
          const maintenanceList = record[dateKey] as WorkCenterMaintenance[] || [];          
          // 检查日期是否在当前月份内
          const isCurrentMonth = moment(currentDate).format('YYYY-MM') === moment(selectedMonth).format('YYYY-MM');
          
          // 检查是否是周末
          const isWeekend = moment(currentDate).day() === 0 || moment(currentDate).day() === 6;
          
          return (
            <div 
              style={{
                height: 100,
                display: 'flex',
                flexDirection: 'column',
                backgroundColor: isCurrentMonth ? (isWeekend ? '#f5f5f5' : '#ffffff') : '#f0f0f0',
                overflow: 'hidden'
              }}
            >
              <div style={{ fontSize: '14px', padding: '2px', fontWeight: 'bold', textAlign: 'center', borderBottom: '1px solid #eee' }}>
                {moment(currentDate).format('D')}
              </div>
              <div style={{ flex: 1, overflowY: 'auto', fontSize: '12px', textAlign: 'center', padding: '2px' }}>
                {maintenanceList.length > 0 ? (
                  maintenanceList.map((item, index) => (
                    <div 
                      key={item.id || `maintenance_${index}`} 
                      style={{ marginBottom: 0, padding: '2px', cursor: 'pointer', border: '1px solid transparent', borderRadius: '4px' }}
                      onClick={(e) => {
                      e.stopPropagation();
                      handleMaintenanceClick(item);
                    }}
                      onMouseEnter={(e) => {
                        e.stopPropagation();
                        e.currentTarget.style.borderColor = '#1890ff';
                      }}
                      onMouseLeave={(e) => {
                        e.stopPropagation();
                        e.currentTarget.style.borderColor = 'transparent';
                      }}
                    >
                      <Tag color={item.status === 'Active' ? 'green' : 'red'} style={{ fontSize: '10px' }}>
                        {item.status}
                      </Tag>
                      <div style={{ fontSize: '11px', marginBottom: '1px' }}>
                        {item.startTime ? moment(item.startTime).format('HH:mm') : '--'} - {item.endTime ? moment(item.endTime).format('HH:mm') : '--'}
                      </div>
                      <div style={{ fontSize: '10px', color: '#666', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {item.description}
                      </div>
                    </div>
                  ))
                ) : (
                  <div style={{ color: '#999', textAlign: 'center', marginTop: '10px', fontSize: '12px' }}>无维护计划</div>
                )}
              </div>
            </div>
          );
        }
      });
    });
    return columns;
  }, [selectedWorkCenter, selectedMonth, handleMaintenanceClick]);

  // 获取当前月份的日期范围
  const getMonthRange = () => {
    const startDate = moment(selectedMonth).startOf('month');
    const endDate = moment(selectedMonth).endOf('month');
    return `${startDate.format('YYYY-MM-DD')} 至 ${endDate.format('YYYY-MM-DD')}`;
  };

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0, fontSize: 24 }}>
          <CalendarOutlined style={{ marginRight: 8 }} />
          工作中心维护日历
        </h1>
        <Select
          value={moment(selectedMonth).format('YYYY-MM-DD')}
          onChange={handleMonthSelect}
          options={monthOptions}
          style={{ width: 200 }}
        />
      </div>

      <div style={{ display: 'flex', gap: 24, height: 'calc(100vh - 160px)' }}>
        {/* 左侧工作中心列表 */}
        <Card
          title="工作中心列表"
          style={{ width: 200, overflow: 'hidden', display: 'flex', flexDirection: 'column', height: '100%' }}
        >
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <Spin spinning={workCenterLoading} indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />}>
              <List
                dataSource={workCenters}
                renderItem={(item) => (
                  <List.Item
                    key={item.workCenterCode}
                    onClick={() => handleWorkCenterSelect(item.workCenterCode)}
                    style={{
                      cursor: 'pointer',
                      borderLeft: selectedWorkCenter === item.workCenterCode ? '3px solid #1890ff' : 'none',
                      backgroundColor: selectedWorkCenter === item.workCenterCode ? '#e6f7ff' : 'transparent',
                      paddingLeft: 12
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 'bold' }}>{item.description}</div>
                      <div style={{ fontSize: 12, color: '#666' }}>{item.workCenterCode}</div>
                    </div>
                  </List.Item>
                )}
                style={{ overflowY: 'auto', height: '100%', maxHeight: 'calc(100vh - 260px)', padding: 0 }}
              />
            </Spin>
          </div>
        </Card>

        {/* 右侧日历表格 */}
        <Card
          title={`${moment(selectedMonth).format('YYYY年MM月')} 维护计划 (${getMonthRange()})`}
          style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
        >
          <Spin spinning={loading} indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />}>
            <Table
              dataSource={calendarData}
              columns={tableColumns}
              pagination={false}
              scroll={{ y: 'calc(100vh - 300px)', x: 'max-content' }}
              rowKey="key"
              bordered
            />
          </Spin>
        </Card>
      </div>

      {/* 维护记录详情模态框 */}
      <Modal
        title="维护记录详情"
        width={416}
        open={isModalVisible}
        onCancel={handleModalCancel}
        footer={[
          <Button key="cancel" onClick={handleModalCancel}>
            取消
          </Button>,
          <Button key="save" type="primary" onClick={handleFormSubmit} loading={isSubmitting}>
            保存修改
          </Button>
        ]}
      >
        {selectedMaintenance && (
          <div style={{ textAlign: 'left' }}>
            <div style={{ marginBottom: '16px', marginLeft: '8px' }}>
              <p style={{ margin: '8px 0' }}>工作中心: {selectedMaintenance.description} ({selectedMaintenance.workCenterCode})</p>
              <p style={{ margin: '8px 0' }}>日期: {selectedMaintenance.localDate}</p>
            </div>
            <Form form={form} layout="horizontal" labelAlign="left" labelCol={{ span: 7 }} wrapperCol={{ span: 17 }} style={{ marginLeft: '8px' }}>
              <Form.Item label="容量" name="capacity" rules={[{ required: true, message: '请输入容量' }]}>
                <InputNumber disabled  min={0} step={1} />
              </Form.Item>
              <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
                <Select
                 style={{ width: '50%' }}>
                  <Select.Option value="Active">激活</Select.Option>
                  <Select.Option value="Inactive">未激活</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item 
                label="开始时间" 
                name="startTime" 
                rules={[{ required: true, message: '请选择开始时间' }]}
                getValueFromEvent={(date) => date}
                getValueProps={(value) => ({ value: value || undefined })}
              >
                <TimePicker 
                  format="HH:mm" 
                  minuteStep={30} 
                  style={{ width: '50%' }}
                />
              </Form.Item>
              <Form.Item 
                label="结束时间" 
                name="endTime" 
                rules={[{ required: true, message: '请选择结束时间' }]}
                getValueFromEvent={(date) => date}
                getValueProps={(value) => ({ value: value || undefined })}
              >
                <TimePicker 
                  format="HH:mm" 
                  minuteStep={30} 
                  style={{ width: '50%' }}
                />
              </Form.Item>
            </Form>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default WorkCenterCalendarPage;
