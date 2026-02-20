/**
 * 学生能力画像聚合表
 * @description 由成绩事实数据衍生的学生综合画像，定期刷新
 */
import { buildStudentDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'AggStudentProfileModel',
    caption: '学生画像',
    description: '学生能力画像聚合表，基于成绩事实数据定期刷新。包含综合等级、成绩趋势和 AI 生成的完整画像摘要（JSON 格式）。',
    tableName: 'agg_student_profile',
    idColumn: 'profile_id',

    dimensions: [
        buildStudentDim()
    ],

    properties: [
        { column: 'profile_id', caption: '画像ID', type: 'BIGINT' },
        { column: 'overall_level', caption: '综合等级', description: 'A=优秀, B=良好, C=中等, D=待提升' },
        { column: 'score_trend', caption: '成绩趋势', description: 'rising=上升, stable=稳定, declining=下降' },
        { column: 'ai_summary', caption: 'AI画像摘要', description: 'AI 生成的完整画像 JSON' },
        { column: 'refreshed_at', caption: '最后刷新时间', type: 'DATETIME' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
