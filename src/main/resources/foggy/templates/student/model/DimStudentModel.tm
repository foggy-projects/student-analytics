/**
 * 学生维度表
 * @description 学生基本信息，关联班级维度
 */
import { buildClassDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'DimStudentModel',
    caption: '学生',
    description: '学生维度表，包含学号、姓名、性别、出生日期、入学日期、学籍状态。关联班级维度支持按班级/年级聚合。',
    tableName: 'dim_student',
    idColumn: 'student_id',

    dimensions: [
        buildClassDim()
    ],

    properties: [
        { column: 'student_id', caption: '学生ID', type: 'BIGINT' },
        { column: 'student_no', caption: '学号' },
        { column: 'student_name', caption: '姓名' },
        { column: 'gender', caption: '性别', description: 'M=男, F=女' },
        { column: 'birth_date', caption: '出生日期', type: 'DAY' },
        { column: 'enroll_date', caption: '入学日期', type: 'DAY' },
        { column: 'phone', caption: '联系电话' },
        { column: 'student_status', caption: '学籍状态', description: 'active=在读, graduated=毕业, transferred=转学' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
