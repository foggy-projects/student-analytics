/**
 * 日期维度表
 * @description 日期维度，支持按年/月/周/学期切片分析
 */
import { buildSemesterDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'DimDateModel',
    caption: '日期',
    description: '日期维度表，预生成日期属性（年、月、周几、是否教学日），关联学期维度。',
    tableName: 'dim_date',
    idColumn: 'date_id',

    dimensions: [
        buildSemesterDim()
    ],

    properties: [
        { column: 'date_id', caption: '日期ID', description: '格式 2024-01-15' },
        { column: 'date_value', caption: '日期', type: 'DAY' },
        { column: 'year', caption: '年', type: 'INTEGER' },
        { column: 'month', caption: '月', type: 'INTEGER' },
        { column: 'day', caption: '日', type: 'INTEGER' },
        { column: 'week_day', caption: '周几', type: 'INTEGER', description: '1=周一, 7=周日' },
        { column: 'week_of_year', caption: '年中第几周', type: 'INTEGER' },
        { column: 'is_school_day', caption: '是否教学日', type: 'BOOLEAN' }
    ]
};
