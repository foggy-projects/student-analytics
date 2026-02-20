/**
 * 学习建议事实表
 * @description AI 或规则生成的个性化学习建议
 */
import { buildStudentDim, buildSubjectDim, buildKnowledgePointDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'FactLearningAdviceModel',
    caption: '学习建议',
    description: '学习建议事实表，记录为学生生成的个性化学习建议。支持按科目和知识点定位，区分 AI 生成和规则生成，包含状态流转（待处理/已完成/已忽略）。',
    tableName: 'fact_learning_advice',
    idColumn: 'advice_id',

    dimensions: [
        buildStudentDim(),
        buildSubjectDim(),
        buildKnowledgePointDim({ caption: '关联知识点' })
    ],

    properties: [
        { column: 'advice_id', caption: '建议ID', type: 'BIGINT' },
        { column: 'advice_type', caption: '建议类型', description: 'review=复习, practice=练习, consolidate=巩固, extend=拓展' },
        { column: 'advice_level', caption: '紧急程度', description: 'high=紧急, medium=一般, low=建议' },
        { column: 'advice_content', caption: '建议内容' },
        { column: 'generate_type', caption: '生成方式', description: 'ai=AI生成, rule=规则生成' },
        { column: 'status', caption: '状态', description: 'pending=待处理, done=已完成, ignored=已忽略' },
        { column: 'feedback', caption: '老师反馈' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' },
        { column: 'expires_at', caption: '有效期', type: 'DATETIME' }
    ]
};
