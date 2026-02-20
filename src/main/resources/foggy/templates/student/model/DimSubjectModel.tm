/**
 * 科目维度表
 * @description 考试科目信息，包含分数线配置
 */
export const model = {
    name: 'DimSubjectModel',
    caption: '科目',
    description: '科目维度表，定义科目名称、类型（主科/副科）、满分、及格分、优秀分等分数线配置。',
    tableName: 'dim_subject',
    idColumn: 'subject_id',

    properties: [
        { column: 'subject_id', caption: '科目ID', type: 'BIGINT' },
        { column: 'subject_name', caption: '科目名称' },
        { column: 'subject_type', caption: '科目类型', description: 'main=主科, minor=副科' },
        { column: 'full_score', caption: '满分', type: 'INTEGER' },
        { column: 'pass_score', caption: '及格分', type: 'INTEGER' },
        { column: 'excellent_score', caption: '优秀分', type: 'INTEGER' },
        { column: 'is_exam_subject', caption: '是否考试科目', type: 'BOOLEAN' },
        { column: 'sort_order', caption: '排序', type: 'INTEGER' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
