import React, {useEffect, useState, useCallback} from 'react';
import {Card, message, Spin, Table, Form, Input, DatePicker, Row, Col, Button, Space, Typography} from 'antd';
import type {ColumnType} from 'antd/es/table';
import {queryTimeslots} from '../services/api';
import type {Timeslot} from '../services/model';
import moment from 'moment';
import {SearchOutlined, FilterOutlined} from '@ant-design/icons';

const { Text } = Typography;

interface TaskTimeslot {
    orderNo: string;
    taskNo: string;
    contractNum: string;
    productCode: string;
    productName: string;
    timeslots: Timeslot[];
}

interface TaskData {
    [key: string]: {
        timeslots: Timeslot[];
        dateMap: Map<string, Timeslot[]>;
        orderNo?: string;
        contractNum?: string;
        productCode?: string;
        productName?: string;
    };
}

interface TableData {
    key: string;
    taskNo: string;
    orderNo?: string;
    contractNum?: string;
    productCode?: string;
    productName?: string;
    allTimeslots?: Timeslot[];

    [dateKey: string]: string | Timeslot[] | undefined;
}

// 调度时序表页面组件 - 用于展示生产调度的时间轴视图
const SchedulingTimelinePage: React.FC = () => {
    // 表单实例，用于管理查询条件
    const [form] = Form.useForm();
    // 加载状态，用于控制加载动画显示
    const [loading, setLoading] = useState(false);
    // 表格数据，存储调度时序信息
    const [tableData, setTableData] = useState<TableData[]>([]);

    // 分页状态
    const [currentPage, setCurrentPage] = useState(1); // 当前页码
    const [pageSize, setPageSize] = useState(20); // 每页记录数
    const [total, setTotal] = useState(0); // 总记录数
    // 筛选状态，控制筛选条件的展开/收起
    const [filterVisible, setFilterVisible] = useState(false);

    /**
     * 渲染时间轴组件
     * @param record 表格行数据，包含任务的所有时间槽信息
     * @returns 渲染好的时间轴JSX元素
     */
    const renderTimeline = (record: TableData) => {
        // 从allTimeslots中获取该任务的所有时间槽
        const uniqueTimeslots = record.allTimeslots || [];

        // 按开始时间排序时间槽，确保时间轴上的工序按时间顺序排列
        const sortedTimeslots = [...uniqueTimeslots].sort((a, b) => {
            if (!a.startTime) return 1; // 无开始时间的排在后面
            if (!b.startTime) return -1; // 有开始时间的排在前面
            return new Date(a.startTime).getTime() - new Date(b.startTime).getTime(); // 按开始时间升序排列
        });

        // 计算时间轴范围（最早开始时间和最晚结束时间）
        let minTime: Date | null = null; // 时间轴起始时间
        let maxTime: Date | null = null; // 时间轴结束时间

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
        if (!maxTime) maxTime = new Date(minTime.getTime() + 2 * 60 * 60 * 1000); // 默认2小时时间范围

        // 计算时间轴总长度（毫秒）
        const timeRange = maxTime.getTime() - minTime.getTime();

        /**
         * 为工序分配颜色
         * @param timeslot 时间槽对象
         * @returns 分配给该工序的颜色值
         */
        const getProcedureColor = (timeslot: Timeslot) => {
            const procedure = timeslot.procedure;

            // 定义颜色数组，用于区分不同工序
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

            // 生成哈希值
            let hash = 0;
            for (let i = 0; i < key.length; i++) {
                hash = key.charCodeAt(i) + ((hash << 5) - hash); // 计算简单的哈希值
            }

            // 将哈希值映射到颜色数组索引
            const index = Math.abs(hash) % colors.length;
            return colors[index];
        };

        /**
         * 计算时间块在时间轴上的位置和宽度
         * @param ts 时间槽对象
         * @returns 包含left和width百分比值的对象
         */
        const getPositionAndWidth = (ts: Timeslot) => {
            if (!ts.startTime) return {left: '0%', width: '100%'}; // 无开始时间的时间块占满整个时间轴

            const startTime = new Date(ts.startTime);
            const endTime = ts.endTime ? new Date(ts.endTime) : new Date(startTime.getTime() + 60 * 60 * 1000); // 默认1小时持续时间

            // 计算时间块的起始位置（相对于时间轴的百分比）
            const left = ((startTime.getTime() - minTime!.getTime()) / timeRange) * 100;
            // 计算时间块的宽度（相对于时间轴的百分比）
            const width = ((endTime.getTime() - startTime.getTime()) / timeRange) * 100;

            // 确保时间块不会超出时间轴范围
            const adjustedLeft = Math.max(0, left); // 左边界不小于0%
            const adjustedWidth = Math.min(100 - adjustedLeft, Math.max(1, width)); // 右边界不大于100%，且宽度至少为1%

            return {
                left: `${adjustedLeft}%`,
                width: `${adjustedWidth}%`
            };
        };

        /**
         * 检测两个时间槽是否重叠
         * @param ts1 第一个时间槽
         * @param ts2 第二个时间槽
         * @returns 如果重叠返回true，否则返回false
         */
        const isOverlapping = (ts1: Timeslot, ts2: Timeslot) => {
            if (!ts1.startTime || !ts2.startTime) return false; // 无开始时间的时间槽不参与重叠检测

            const start1 = new Date(ts1.startTime).getTime();
            const end1 = ts1.endTime ? new Date(ts1.endTime).getTime() : start1 + 60 * 60 * 1000;
            const start2 = new Date(ts2.startTime).getTime();
            const end2 = ts2.endTime ? new Date(ts2.endTime).getTime() : start2 + 60 * 60 * 1000;

            // 检测两个时间段是否重叠：如果一个开始时间小于另一个结束时间，则重叠
            return start1 < end2 && start2 < end1;
        };

        /**
         * 为时间槽分配轨道（行），确保同一轨道上的时间槽不重叠
         * @param timeslots 时间槽数组
         * @returns 轨道数组，每个轨道包含一组不重叠的时间槽
         */
        const assignTracks = (timeslots: Timeslot[]) => {
            const tracks: Timeslot[][] = []; // 轨道数组，每个轨道是一个时间槽数组

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

        // 分配轨道，将重叠的时间槽分配到不同轨道
        const tracks = assignTracks(sortedTimeslots);
        const trackCount = tracks.length; // 轨道数量
        const trackHeight = 50; // 每个轨道的高度（像素）
        const totalHeight = Math.max(80, trackCount * trackHeight); // 总高度，至少80px，确保时间轴有足够高度

        return (
            <div style={{
                height: 'auto',
                minHeight: '150px',
                maxHeight: '300px',
                overflow: 'auto',
                border: '1px solid #f0f0f0',
                borderRadius: '4px',
                padding: '12px'
            }}>
                {sortedTimeslots.length > 0 ? (
                    <div>
                        {/* 时间轴刻度 */}
                        <div style={{
                            height: '20px',
                            marginBottom: '12px',
                            display: 'flex',
                            alignItems: 'center',
                            fontSize: '10px',
                            color: '#999'
                        }}>
                            <div style={{width: '80px', flexShrink: 0}}>时间</div>
                            <div style={{flex: 1, position: 'relative'}}>
                                {/* 时间点 */}
                                <div style={{position: 'absolute', left: '0%', top: '0'}}>
                                    {moment(minTime).format('YYYY-MM-DD HH:mm')}
                                </div>
                                <div style={{position: 'absolute', left: '50%', top: '0'}}>
                                    {moment(new Date(minTime.getTime() + timeRange / 2)).format('YYYY-MM-DD HH:mm')}
                                </div>
                                <div style={{
                                    position: 'absolute',
                                    left: '100%',
                                    top: '0',
                                    transform: 'translateX(-100%)'
                                }}>
                                    {moment(maxTime).format('YYYY-MM-DD HH:mm')}
                                </div>
                            </div>
                        </div>

                        {/* 时间轴和时间块 */}
                        <div style={{display: 'flex', height: totalHeight}}>
                            <div style={{width: '80px', flexShrink: 0}}></div>
                            <div style={{flex: 1, position: 'relative'}}>
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
                                    }}/>
                                ))}

                                {/* 时间块 */}
                                {tracks.map((track, trackIndex) => (
                                    <>
                                        {track.map((ts) => {
                                            const {left, width} = getPositionAndWidth(ts);
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
                                                    }}/>
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
                                                    }}/>

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
                                                    }}/>
                                                </div>
                                            );
                                        })}

                                    </>
                                ))}
                            </div>
                        </div>

                        {/* 工序列表 */}
                        <div style={{marginTop: '12px', fontSize: '11px', color: '#666'}}>
                            <div style={{fontWeight: 'bold', marginBottom: '4px'}}>工序列表：</div>
                            <div style={{display: 'flex', flexDirection: 'column', gap: '8px'}}>
                                {sortedTimeslots.map((ts, index) => (
                                    <div key={ts.id} style={{display: 'flex', flexDirection: 'column', gap: '4px'}}>
                                        <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                                            <div style={{
                                                width: '8px',
                                                height: '8px',
                                                borderRadius: '50%',
                                                background: getProcedureColor(ts)
                                            }}/>
                                            <span>{index + 1}. {ts.procedure?.procedureName || '未知'}</span>
                                            <span style={{color: '#999'}}>
                        ({ts.startTime ? moment(ts.startTime).format('YYYY-MM-DD HH:mm') : '?'}-{ts.endTime ? moment(ts.endTime).format('YYYY-MM-DD HH:mm') : '?'})
                      </span>
                                        </div>
                                        {/* 工序时间轴 */}
                                        <div style={{
                                            marginLeft: '12px',
                                            width: '100%',
                                            height: '10px',
                                            position: 'relative'
                                        }}>
                                            {/* 时间轴背景 */}
                                            <div style={{
                                                width: '100%',
                                                height: '2px',
                                                background: '#f0f0f0',
                                                position: 'absolute',
                                                top: '50%',
                                                transform: 'translateY(-50%)'
                                            }}/>
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
                                                }}/>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                ) : (
                    <div style={{textAlign: 'center', color: '#999', padding: '40px 20px'}}>
                        暂无时序数据
                    </div>
                )}
            </div>
        );
    };

    /**
     * 动态生成表格列配置
     * @returns 表格列配置数组
     */
    const generateColumns = () => {
        const columns: ColumnType<TableData>[] = [
            {
                title: '任务信息',
                dataIndex: 'taskNo',
                key: 'taskNo',
                width: 80,
                fixed: 'left' as const, // 固定在左侧，方便查看任务信息
                render: (_, record) => (
                    <div>
                        <div style={{fontWeight: 'bold', marginBottom: 4}}>任务:{record.taskNo}</div>
                        <div style={{fontSize: 12, color: '#666'}}>合同号: {record.contractNum}</div>
                        <div style={{fontSize: 12, color: '#666'}}>产品号: {record.productCode}</div>
                        <div style={{fontSize: 12, color: '#666'}}>产品名: {record.productName}</div>
                    </div>
                ),
            },
            {
                title: '时序图',
                key: 'timeline',
                width: 500,
                fixed: 'left' as const, // 固定在左侧，与任务号一起显示
                align: 'center' as const,
                render: (_, record) => renderTimeline(record) // 渲染时间轴组件
            },
        ];

        return columns;
    };

    /**
     * 获取时间槽跨越的所有日期
     * @param startTime 开始时间字符串
     * @param endTime 结束时间字符串
     * @returns 日期字符串数组，格式为YYYY-MM-DD
     */
    const getDatesInRange = useCallback((startTime: string, endTime: string): string[] => {
        const startDate = moment(startTime.substring(0, 10)); // 提取日期部分
        const endDate = moment(endTime.substring(0, 10)); // 提取日期部分
        const dates: string[] = [];

        // 遍历开始日期到结束日期的所有日期
        const currentDate = moment(startDate);
        while (currentDate.isSameOrBefore(endDate, 'day')) {
            dates.push(currentDate.format('YYYY-MM-DD'));
            currentDate.add(1, 'day'); // 增加一天
        }

        return dates;
    }, []);

    /**
     * 根据任务号分组数据
     * @param taskTimeslots 任务时间槽数组
     * @returns 按任务号分组后的任务数据对象
     */
    const groupTimeslotsByTask = useCallback((taskTimeslots: TaskTimeslot[]): TaskData => {
        return taskTimeslots.reduce((acc, taskTimeslot) => {
            const key = taskTimeslot.taskNo; // 以任务号为分组键
            if (!acc[key]) {
                // 如果该任务号尚未分组，初始化分组数据
                acc[key] = {
                    timeslots: [],
                    orderNo: taskTimeslot.orderNo,
                    contractNum: taskTimeslot.contractNum || '',
                    productCode: taskTimeslot.productCode || '',
                    productName: taskTimeslot.productName || '',
                    dateMap: new Map()};
            }

            // 处理每个时间槽
            taskTimeslot.timeslots.forEach(timeslot => {
                acc[key].timeslots.push(timeslot); // 添加到任务的所有时间槽列表

                // 按日期分组时间槽
                if (timeslot.startTime && timeslot.endTime) {
                    const dates = getDatesInRange(timeslot.startTime, timeslot.endTime); // 获取时间槽跨越的所有日期
                    dates.forEach(date => {
                        if (!acc[key].dateMap.has(date)) {
                            acc[key].dateMap.set(date, []); // 初始化该日期的时间槽列表
                        }
                        acc[key].dateMap.get(date)?.push(timeslot); // 添加时间槽到对应日期
                    });
                } else {
                    // 处理没有时间信息的时间槽，添加到当前日期
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

    /**
     * 提取所有日期
     * @param taskTimeslots 任务时间槽数组
     * @returns 排序后的日期字符串数组
     */
    const extractDates = (taskTimeslots: TaskTimeslot[]): string[] => {
        const dateSet = new Set<string>(); // 使用Set去重
        const today = moment().format('YYYY-MM-DD');

        taskTimeslots.forEach(taskTimeslot => {
            let hasValidTimeslot = false;

            taskTimeslot.timeslots.forEach(timeslot => {
                if (timeslot.startTime) {
                    const startDate = timeslot.startTime.substring(0, 10); // 提取日期部分
                    dateSet.add(startDate);
                    hasValidTimeslot = true;
                }
                if (timeslot.endTime) {
                    const endDate = timeslot.endTime.substring(0, 10); // 提取日期部分
                    dateSet.add(endDate);
                    hasValidTimeslot = true;
                }
            });

            // 如果任务没有有效的时间槽，添加今天的日期作为默认
            if (!hasValidTimeslot) {
                dateSet.add(today);
            }
        });

        // 按日期排序并返回
        return Array.from(dateSet).sort();
    };

    /**
     * 构建表格数据
     * @param groupedData 按任务号分组后的数据
     * @param dates 日期数组
     * @returns 表格数据数组
     */
    const buildTableData = (groupedData: TaskData, dates: string[]): TableData[] => {
        return Object.entries(groupedData).map(([taskNo, data]) => {
            // 构建表格行数据
            const row: TableData = {
                key: taskNo, // 将任务号作为key
                taskNo:taskNo,
                orderNo: data.orderNo,
                contractNum: data.contractNum || '',
                productName: data.productName || '',
                productCode: data.productCode || '',
                allTimeslots: data.timeslots // 保存该任务的所有时间槽
            };

            // 为每个日期添加列数据
            dates.forEach(date => {
                row[date] = data.dateMap.get(date) || []; // 获取该日期的时间槽列表
            });

            return row;
        });
    };

    /**
     * 处理搜索事件
     * 当用户点击搜索按钮时触发，根据表单条件查询数据
     */
    const handleSearch = async () => {
        const values = form.getFieldsValue(); // 获取表单值
        // 处理日期范围
        let startTime: string | undefined;
        let endTime: string | undefined;
        if (values.dateRange && values.dateRange.length === 2) {
            startTime = values.dateRange[0].format('YYYY-MM-DD'); // 开始日期
            endTime = values.dateRange[1].format('YYYY-MM-DD'); // 结束日期
        }

        // 重置到第一页，搜索结果从第一页开始显示
        setCurrentPage(1);
        // 调用fetchData获取数据
        await fetchData({
            ...values,
            startTime,
            endTime,
            pageNum: 1,
            pageSize
        });
    };

    /**
     * 处理重置事件
     * 当用户点击重置按钮时触发，清空表单条件并重新查询数据
     */
    const handleReset = async () => {
        form.resetFields(); // 重置表单
        // 重置到第一页
        setCurrentPage(1);
        // 调用fetchData获取数据，不传递任何筛选条件
        await fetchData({
            pageNum: 1,
            pageSize
        });
    };

    /**
     * 处理分页变化事件
     * 当用户切换页码或每页显示条数时触发
     * @param page 新的页码
     * @param size 新的每页显示条数
     */
    const handlePaginationChange = async (page: number, size: number) => {
        setCurrentPage(page); // 更新当前页码
        setPageSize(size); // 更新每页显示条数
        const values = form.getFieldsValue(); // 获取当前表单值
        // 处理日期范围
        let startTime: string | undefined;
        let endTime: string | undefined;
        if (values.dateRange && values.dateRange.length === 2) {
            startTime = values.dateRange[0].format('YYYY-MM-DD');
            endTime = values.dateRange[1].format('YYYY-MM-DD');
        }

        // 调用fetchData获取数据，使用新的页码和每页显示条数
        await fetchData({
            ...values,
            startTime,
            endTime,
            pageNum: page,
            pageSize: size
        });
    };

    /**
     * 获取数据并预处理
     * 调用API获取时间槽数据，并进行分组、提取日期和构建表格数据等预处理
     * @param params 查询参数
     */
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
        setLoading(true); // 设置加载状态为true
        try {
            // 调用API获取时间槽数据
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

            // 检查响应是否成功
            if (response.code === 200 && response.data) {
                const taskTimeslots = response.data.content || []; // 获取任务时间槽列表
                // 按订单号和任务号分组数据
                const grouped = groupTimeslotsByTask(taskTimeslots);
                // 提取所有日期
                const dates = extractDates(taskTimeslots);
                // 构建表格数据
                const table = buildTableData(grouped, dates);
                // 设置总记录数为分组后的数据条数，解决分页问题
                setTotal(response.data.totalElements|| 0);
                // 更新表格数据
                setTableData(table);
            } else {
                message.error('网络请求失败');
            }
        } catch (error) {
            // 捕获异常并显示错误信息
            message.error('网络请求失败: ' + (error instanceof Error ? error.message : '未知错误'));
        } finally {
            setLoading(false); // 设置加载状态为false
        }
    };

    /**
     * 组件挂载时初始化加载数据
     * 使用useEffect钩子，依赖为空数组，仅在组件挂载时执行一次
     */
    useEffect(() => {
        // 初始加载数据，使用默认分页参数
        fetchData({
            pageNum: currentPage,
            pageSize
        });
    }, []); // 空依赖数组，仅在组件挂载时执行一次


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
                            <Form.Item name="taskNo" label="任务编号" style={{ marginBottom: 4 }}>
                                <Input placeholder="请输入任务编号" size="small" style={{ height: 24 }}/>
                            </Form.Item>
                        </Col>
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

                        {filterVisible && (
                            <>
                                <Col xs={24} sm={12} md={8} lg={6}>
                                    <Form.Item name="contractNum" label="合同编号" style={{ marginBottom: 4 }}>
                                        <Input placeholder="请输入合同编号" size="small" style={{ height: 24 }}/>
                                    </Form.Item>
                                </Col>
                                <Col xs={24} sm={24} md={16} lg={12}>
                                    <Form.Item name="dateRange" label="日期范围" style={{ marginBottom: 4 }}>
                                        <DatePicker.RangePicker style={{width: '100%', height: 24 }} size="small"/>
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

                <Spin spinning={loading}>
                <Card style={{borderRadius: 6, boxShadow: '0 1px 3px rgba(0,0,0,0.1)'}}>
                    <Table
                        columns={generateColumns()}
                        dataSource={tableData}
                        scroll={{x: 'max-content', y: 500}}
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
                        size="small"
                        bordered
                        rowKey="key"
                        className="scheduling-timeline-table"
                        style={{borderCollapse: 'collapse'}}
                    />
                </Card>
                {!loading && tableData.length === 0 && (
                    <div style={{textAlign: 'center', padding: '32px', color: '#999', fontSize: '14px'}}>
                        暂无调度数据
                    </div>
                )}
                </Spin>

                {/* 外协工序时间槽拆分弹窗 */}

            </div>
        </div>
    );
};

export default SchedulingTimelinePage;
