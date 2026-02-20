/**
 * 用户维度表
 * @description 系统用户信息，包含管理员和教师账号
 */
import { buildTeacherDim } from './dimensions/common-dims.fsscript';

export const model = {
    name: 'DimUserModel',
    caption: '用户',
    description: '系统用户表，包含管理员和教师账号。用户类型分为 admin（管理员）和 teacher（教师），教师用户关联教师表。',
    tableName: 'dim_user',
    idColumn: 'user_id',

    dimensions: [
        buildTeacherDim({ caption: '关联教师' })
    ],

    properties: [
        { column: 'user_id', caption: '用户ID', type: 'BIGINT' },
        { column: 'user_name', caption: '用户名' },
        { column: 'user_type', caption: '用户类型', description: 'admin=管理员, teacher=教师' },
        { column: 'status', caption: '状态', description: 'active=启用, inactive=禁用' },
        { column: 'last_login_at', caption: '最后登录时间', type: 'DATETIME' },
        { column: 'created_at', caption: '创建时间', type: 'DATETIME' }
    ]
};
