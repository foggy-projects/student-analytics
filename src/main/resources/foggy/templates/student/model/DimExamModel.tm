/**
 * 考试维度表
 * @description 考试信息，关联学期维度
 */
import { buildSemesterDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'DimExamModel',
    caption: '考试',
    description: '考试维度表，记录考试名称、类型（随堂测/单元测/期中/期末）、考试日期，关联学期维度。',
    tableName: 'dim_exam',
    idColumn: 'exam_id',

    dimensions: [
        buildSemesterDim()
    ],

    properties: [
        { column: 'exam_id', caption: '考试ID', type: 'BIGINT' },
        { column: 'exam_name', caption: '考试名称' },
        { column: 'exam_type', caption: '考试类型', description: 'daily=随堂测, unit=单元测, midterm=期中, final=期末' },
        { column: 'exam_date', caption: '考试日期', type: 'DAY' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
