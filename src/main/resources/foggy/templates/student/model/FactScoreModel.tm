/**
 * 成绩事实表（核心）
 * @description 学生考试成绩记录，排名通过 QM 窗口函数实时计算
 */
import { buildStudentDim, buildClassDim, buildSubjectDim, buildExamDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'FactScoreModel',
    caption: '成绩',
    description: '成绩事实表，记录每个学生每次考试每科的得分和等级。排名不存储，通过 QM 查询模型的窗口函数实时计算。支持按班级、科目、考试等多维度聚合分析。',
    tableName: 'fact_score',
    idColumn: 'score_id',

    dimensions: [
        buildStudentDim(),
        buildClassDim(),
        buildSubjectDim(),
        buildExamDim()
    ],

    properties: [
        { column: 'score_id', caption: '成绩ID', type: 'BIGINT' },
        { column: 'score_level', caption: '成绩等级', description: 'A=优秀, B=良好, C=及格, D=不及格' }
    ],

    measures: [
        { column: 'score', caption: '得分', type: 'DECIMAL', aggregation: 'avg' }
    ]
};
