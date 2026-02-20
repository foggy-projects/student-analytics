/**
 * 知识点闭包表
 * @description 知识点层级关系闭包表，用于语义层层级聚合查询
 */
import { buildKnowledgePointDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'KnowledgePointClosureModel',
    caption: '知识点层级关系',
    description: '知识点闭包表，存储所有祖先-后代关系及层级距离。distance=0 表示自身，用于支持知识点的多层级聚合查询。',
    tableName: 'knowledge_point_closure',
    idColumn: ['parent_id', 'point_id'],

    dimensions: [
        buildKnowledgePointDim({ name: 'parent', foreignKey: 'parent_id', caption: '祖先知识点' }),
        buildKnowledgePointDim({ name: 'child', foreignKey: 'point_id', caption: '后代知识点' })
    ],

    properties: [
        { column: 'parent_id', caption: '祖先节点ID', type: 'BIGINT' },
        { column: 'point_id', caption: '后代节点ID', type: 'BIGINT' },
        { column: 'distance', caption: '层级距离', type: 'INTEGER', description: '0=自身, 1=直接子节点, 2=孙节点...' }
    ]
};
