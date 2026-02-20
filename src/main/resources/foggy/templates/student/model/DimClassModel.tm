/**
 * 班级维度表
 * @description 班级信息，关联年级维度
 */
import { buildGradeDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'DimClassModel',
    caption: '班级',
    description: '班级维度表，包含班级名称、教室、人数，关联年级维度支持按年级聚合。',
    tableName: 'dim_class',
    idColumn: 'class_id',

    dimensions: [
        buildGradeDim()
    ],

    properties: [
        { column: 'class_id', caption: '班级ID', type: 'BIGINT' },
        { column: 'class_name', caption: '班级名称' },
        { column: 'classroom', caption: '教室' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ],

    measures: [
        { column: 'student_count', caption: '班级人数', type: 'INTEGER', aggregation: 'sum' }
    ]
};
