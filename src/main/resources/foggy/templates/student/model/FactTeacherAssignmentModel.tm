/**
 * 教师任课关系事实表
 * @description 教师-班级-科目的任课分配关系
 */
import { buildTeacherDim, buildClassDim, buildSubjectDim, buildSemesterDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'FactTeacherAssignmentModel',
    caption: '教师任课关系',
    description: '教师任课分配事实表，记录教师与班级、科目的对应关系。is_head_teacher 标记班主任，is_current 标记当前有效记录。',
    tableName: 'fact_teacher_assignment',
    idColumn: 'assignment_id',

    dimensions: [
        buildTeacherDim(),
        buildClassDim(),
        buildSubjectDim({ caption: '任教科目' }),
        buildSemesterDim()
    ],

    properties: [
        { column: 'assignment_id', caption: '分配ID', type: 'BIGINT' },
        { column: 'is_head_teacher', caption: '是否班主任', type: 'BOOLEAN' },
        { column: 'is_current', caption: '是否当前', type: 'BOOLEAN' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
