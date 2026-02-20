/**
 * 学期维度表
 * @description 学期时间维度，标记当前学期
 */
export const model = {
    name: 'DimSemesterModel',
    caption: '学期',
    description: '学期维度表，记录学年、学期类型、起止日期。is_current 标记当前活跃学期。',
    tableName: 'dim_semester',
    idColumn: 'semester_id',

    properties: [
        { column: 'semester_id', caption: '学期ID', type: 'BIGINT' },
        { column: 'semester_name', caption: '学期名称' },
        { column: 'school_year', caption: '学年', description: '格式如 2025-2026' },
        { column: 'semester_type', caption: '学期类型', description: 'first=上学期, second=下学期' },
        { column: 'start_date', caption: '开始日期', type: 'DAY' },
        { column: 'end_date', caption: '结束日期', type: 'DAY' },
        { column: 'is_current', caption: '是否当前学期', type: 'BOOLEAN' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
