/**
 * 年级维度表
 * @description 年级信息，区分小学/初中学段
 */
export const model = {
    name: 'DimGradeModel',
    caption: '年级',
    description: '年级维度表，包含年级名称、序号和学段。grade_level: 1-6为小学, 7-9为初中。',
    tableName: 'dim_grade',
    idColumn: 'grade_id',

    properties: [
        { column: 'grade_id', caption: '年级ID', type: 'BIGINT' },
        { column: 'grade_name', caption: '年级名称' },
        { column: 'grade_level', caption: '年级序号', type: 'INTEGER', description: '1-6小学, 7-9初中' },
        { column: 'stage', caption: '学段', description: 'primary=小学, junior=初中' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
