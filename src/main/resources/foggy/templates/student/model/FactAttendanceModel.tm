/**
 * 考勤事实表
 * @description 学生每日考勤记录
 */
import { buildStudentDim, buildClassDim, buildDateDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'FactAttendanceModel',
    caption: '考勤',
    description: '考勤事实表，记录学生每日出勤状态。支持按班级、日期维度聚合，可分析出勤率、迟到率等指标。',
    tableName: 'fact_attendance',
    idColumn: 'attendance_id',

    dimensions: [
        buildStudentDim(),
        buildClassDim(),
        buildDateDim()
    ],

    properties: [
        { column: 'attendance_id', caption: '考勤ID', type: 'BIGINT' },
        { column: 'status', caption: '出勤状态', description: 'present=出勤, absent=缺勤, late=迟到, leave_early=早退, sick_leave=病假' },
        { column: 'time_slot', caption: '时段', description: 'morning=上午, afternoon=下午, evening=晚上' },
        { column: 'reason', caption: '原因' },
        { column: 'recorded_by', caption: '记录人ID', type: 'BIGINT' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
