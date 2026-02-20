/**
 * 教师维度表
 * @description 教师基本信息
 */
export const model = {
    name: 'DimTeacherModel',
    caption: '教师',
    description: '教师基本信息表，包含工号、姓名、性别、联系方式。',
    tableName: 'dim_teacher',
    idColumn: 'teacher_id',

    properties: [
        { column: 'teacher_id', caption: '教师ID', type: 'BIGINT' },
        { column: 'teacher_no', caption: '工号' },
        { column: 'teacher_name', caption: '姓名' },
        { column: 'gender', caption: '性别', description: 'M=男, F=女' },
        { column: 'phone', caption: '联系电话' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
