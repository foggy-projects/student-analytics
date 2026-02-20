/**
 * 知识点维度表
 * @description 知识点树形结构，关联科目和年级维度
 */
import { buildSubjectDim, buildGradeDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'DimKnowledgePointModel',
    caption: '知识点',
    description: '知识点维度表，支持树形层级结构（parent_id 自引用）。包含难度和重要程度评级，关联科目和年级维度。配合 knowledge_point_closure 闭包表实现多层级聚合。',
    tableName: 'dim_knowledge_point',
    idColumn: 'point_id',

    dimensions: [
        buildSubjectDim({ caption: '所属科目' }),
        buildGradeDim({ caption: '适用年级' })
    ],

    properties: [
        { column: 'point_id', caption: '知识点ID', type: 'BIGINT' },
        { column: 'point_code', caption: '知识点编码' },
        { column: 'point_name', caption: '知识点名称' },
        { column: 'parent_id', caption: '父知识点ID', type: 'BIGINT' },
        { column: 'point_level', caption: '层级深度', type: 'INTEGER' },
        { column: 'difficulty', caption: '难度等级', type: 'INTEGER', description: '1-5, 5最难' },
        { column: 'importance', caption: '重要程度', type: 'INTEGER', description: '1-5, 5最重要' },
        { column: 'description', caption: '知识点描述' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
