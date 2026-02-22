#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Student Analytics - 示例数据生成器
明德初级中学 · 贴近真实的演示数据

生成约 6.2 万条记录，输出 sql/seed-data.sql
使用方法: python scripts/generate_seed_data.py

无外部依赖，仅使用 Python 标准库。
"""

import random
import math
import hashlib
import os
from datetime import date, datetime, timedelta
from collections import defaultdict

# ============================================================
# 固定随机种子，确保每次生成完全一致
# ============================================================
SEED = 42
random.seed(SEED)

# ============================================================
# 全局配置
# ============================================================
OUTPUT_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'sql', 'seed-data.sql')
BATCH_SIZE = 200  # 每条 INSERT 语句包含的行数

# ============================================================
# 中文姓名池
# ============================================================
SURNAMES_WEIGHTED = [
    # (姓氏, 权重)
    ('王', 7), ('李', 7), ('张', 7), ('刘', 6), ('陈', 6),
    ('杨', 4), ('黄', 4), ('赵', 4), ('吴', 3), ('周', 3),
    ('徐', 3), ('孙', 3), ('马', 3), ('朱', 3), ('胡', 3),
    ('林', 2), ('何', 2), ('高', 2), ('郭', 2), ('罗', 2),
    ('梁', 2), ('宋', 2), ('唐', 2), ('许', 2), ('邓', 2),
    ('冯', 1), ('韩', 1), ('曹', 1), ('曾', 1), ('谢', 1),
    ('萧', 1), ('程', 1), ('潘', 1), ('袁', 1), ('蒋', 1),
    ('蔡', 1), ('余', 1), ('于', 1), ('叶', 1), ('方', 1),
]

MALE_NAMES = [
    '浩', '杰', '鑫', '磊', '涛', '辉', '勇', '强',
    '子轩', '浩然', '宇航', '志远', '明辉', '俊杰', '天宇', '家豪',
    '嘉俊', '泽宇', '一航', '子墨', '宇辰', '逸飞', '瑞祥', '博文',
    '思远', '文博', '建军', '晓峰', '云飞', '承志', '皓轩', '伟豪',
    '子豪', '旭东', '子涵', '梓睿', '亦辰', '少杰', '鹏飞', '成龙',
    '春林', '国强', '海涛', '永康', '明哲', '德华', '景行', '天翔',
    '星辰', '致远', '立恒', '宏伟', '文杰', '晨曦', '嘉诚', '学文',
    '润泽', '启明', '振华', '昊天',
]

FEMALE_NAMES = [
    '颖', '婷', '洁', '静', '雪', '悦', '萱', '琪',
    '思涵', '雨萱', '欣怡', '诗涵', '梦琪', '雅婷', '子涵', '佳怡',
    '语嫣', '若曦', '心怡', '美玲', '晓彤', '紫萱', '雨晴', '思琪',
    '婉婷', '诗雨', '小燕', '秀英', '慧敏', '丽华', '晓红', '建芳',
    '文静', '嘉欣', '可欣', '芷若', '千雪', '雅琴', '梓萱', '珂馨',
    '怡然', '悦宁', '舒雅', '清荷', '芸熙', '玉洁', '世琳', '瑞雪',
    '语桐', '若兮', '依诺', '月华', '凤仪', '慧兰', '书瑶', '婉清',
    '安然', '沁怡', '雅欣', '晨露',
]

TEACHER_MALE_NAMES = [
    '建国', '志强', '伟明', '永刚', '海波', '明远', '文华', '德胜',
    '国栋', '正平', '学军', '世杰', '春生', '光明', '立志', '大伟',
]

TEACHER_FEMALE_NAMES = [
    '淑芬', '秀兰', '玉华', '丽萍', '桂花', '秀珍', '美华', '慧芳',
    '雅芝', '敏慧', '婷婷', '小红', '翠花', '晓燕', '春梅', '玉兰',
]

# 教师电话前缀
PHONE_PREFIXES = ['138', '139', '136', '137', '158', '159', '188', '189', '155', '186']

# 考勤异常原因模板
LATE_REASONS = ['交通拥堵', '闹钟故障', '家长送达晚', None, None]
ABSENT_REASONS = ['未请假缺勤', None]
SICK_REASONS = ['感冒发烧', '肠胃不适', '牙痛就医', '流感', '头痛', '扁桃体发炎']
LEAVE_EARLY_REASONS = ['身体不适提前离校', '家长接走', None]

# ============================================================
# 学校结构配置
# ============================================================
GRADES = [
    {'grade_id': 1, 'grade_name': '七年级', 'grade_level': 7, 'stage': 'junior'},
    {'grade_id': 2, 'grade_name': '八年级', 'grade_level': 8, 'stage': 'junior'},
    {'grade_id': 3, 'grade_name': '九年级', 'grade_level': 9, 'stage': 'junior'},
]

CLASS_NAMES = {
    1: [('七(1)班', '101'), ('七(2)班', '102'), ('七(3)班', '103'), ('七(4)班', '104')],
    2: [('八(1)班', '201'), ('八(2)班', '202'), ('八(3)班', '203'), ('八(4)班', '204')],
    3: [('九(1)班', '301'), ('九(2)班', '302'), ('九(3)班', '303'), ('九(4)班', '304')],
}

SUBJECTS = [
    {'subject_id': 1, 'subject_name': '语文', 'subject_type': 'main', 'full_score': 120, 'pass_score': 72, 'excellent_score': 102, 'is_exam_subject': True, 'sort_order': 1},
    {'subject_id': 2, 'subject_name': '数学', 'subject_type': 'main', 'full_score': 120, 'pass_score': 72, 'excellent_score': 102, 'is_exam_subject': True, 'sort_order': 2},
    {'subject_id': 3, 'subject_name': '英语', 'subject_type': 'main', 'full_score': 120, 'pass_score': 72, 'excellent_score': 102, 'is_exam_subject': True, 'sort_order': 3},
    {'subject_id': 4, 'subject_name': '物理', 'subject_type': 'main', 'full_score': 100, 'pass_score': 60, 'excellent_score': 85, 'is_exam_subject': True, 'sort_order': 4},
    {'subject_id': 5, 'subject_name': '化学', 'subject_type': 'main', 'full_score': 100, 'pass_score': 60, 'excellent_score': 85, 'is_exam_subject': True, 'sort_order': 5},
    {'subject_id': 6, 'subject_name': '道德与法治', 'subject_type': 'minor', 'full_score': 100, 'pass_score': 60, 'excellent_score': 85, 'is_exam_subject': True, 'sort_order': 6},
    {'subject_id': 7, 'subject_name': '历史', 'subject_type': 'minor', 'full_score': 100, 'pass_score': 60, 'excellent_score': 85, 'is_exam_subject': True, 'sort_order': 7},
    {'subject_id': 8, 'subject_name': '地理', 'subject_type': 'minor', 'full_score': 100, 'pass_score': 60, 'excellent_score': 85, 'is_exam_subject': True, 'sort_order': 8},
    {'subject_id': 9, 'subject_name': '生物', 'subject_type': 'minor', 'full_score': 100, 'pass_score': 60, 'excellent_score': 85, 'is_exam_subject': True, 'sort_order': 9},
]

# 年级可考科目 (subject_id 列表)
GRADE_SUBJECTS = {
    7: [1, 2, 3, 6, 7, 8, 9],       # 七年级: 语数英 + 道法历地生
    8: [1, 2, 3, 4, 6, 7, 8, 9],    # 八年级: + 物理
    9: [1, 2, 3, 4, 5, 6, 7],       # 九年级: + 化学, - 地理生物
}

# 主科 subject_id (daily/unit 考试仅考主科)
MAIN_SUBJECTS = {
    7: [1, 2, 3],           # 语数英
    8: [1, 2, 3, 4],        # 语数英物
    9: [1, 2, 3, 4, 5],     # 语数英物化
}

# 学期配置
SEMESTERS = [
    {'semester_id': 1, 'semester_name': '2024-2025学年第一学期', 'school_year': '2024-2025', 'semester_type': 'first', 'start_date': date(2024, 9, 1), 'end_date': date(2025, 1, 17), 'is_current': False},
    {'semester_id': 2, 'semester_name': '2024-2025学年第二学期', 'school_year': '2024-2025', 'semester_type': 'second', 'start_date': date(2025, 2, 17), 'end_date': date(2025, 7, 4), 'is_current': False},
    {'semester_id': 3, 'semester_name': '2025-2026学年第一学期', 'school_year': '2025-2026', 'semester_type': 'first', 'start_date': date(2025, 9, 1), 'end_date': date(2026, 1, 16), 'is_current': True},
]

# 考试安排
EXAMS = [
    # 学期1
    {'exam_id': 1, 'exam_name': '2024-2025学年第一学期第一次月考', 'exam_type': 'unit', 'exam_date': date(2024, 10, 8), 'semester_id': 1},
    {'exam_id': 2, 'exam_name': '2024-2025学年第一学期随堂测验一', 'exam_type': 'daily', 'exam_date': date(2024, 10, 25), 'semester_id': 1},
    {'exam_id': 3, 'exam_name': '2024-2025学年第一学期期中考试', 'exam_type': 'midterm', 'exam_date': date(2024, 11, 11), 'semester_id': 1},
    {'exam_id': 4, 'exam_name': '2024-2025学年第一学期第二次月考', 'exam_type': 'unit', 'exam_date': date(2024, 12, 6), 'semester_id': 1},
    {'exam_id': 5, 'exam_name': '2024-2025学年第一学期随堂测验二', 'exam_type': 'daily', 'exam_date': date(2024, 12, 20), 'semester_id': 1},
    {'exam_id': 6, 'exam_name': '2024-2025学年第一学期期末考试', 'exam_type': 'final', 'exam_date': date(2025, 1, 10), 'semester_id': 1},
    # 学期2
    {'exam_id': 7, 'exam_name': '2024-2025学年第二学期第一次月考', 'exam_type': 'unit', 'exam_date': date(2025, 3, 14), 'semester_id': 2},
    {'exam_id': 8, 'exam_name': '2024-2025学年第二学期随堂测验一', 'exam_type': 'daily', 'exam_date': date(2025, 4, 3), 'semester_id': 2},
    {'exam_id': 9, 'exam_name': '2024-2025学年第二学期期中考试', 'exam_type': 'midterm', 'exam_date': date(2025, 4, 22), 'semester_id': 2},
    {'exam_id': 10, 'exam_name': '2024-2025学年第二学期第二次月考', 'exam_type': 'unit', 'exam_date': date(2025, 5, 23), 'semester_id': 2},
    {'exam_id': 11, 'exam_name': '2024-2025学年第二学期随堂测验二', 'exam_type': 'daily', 'exam_date': date(2025, 6, 6), 'semester_id': 2},
    {'exam_id': 12, 'exam_name': '2024-2025学年第二学期期末考试', 'exam_type': 'final', 'exam_date': date(2025, 6, 27), 'semester_id': 2},
    # 学期3
    {'exam_id': 13, 'exam_name': '2025-2026学年第一学期第一次月考', 'exam_type': 'unit', 'exam_date': date(2025, 10, 10), 'semester_id': 3},
    {'exam_id': 14, 'exam_name': '2025-2026学年第一学期随堂测验一', 'exam_type': 'daily', 'exam_date': date(2025, 10, 24), 'semester_id': 3},
    {'exam_id': 15, 'exam_name': '2025-2026学年第一学期期中考试', 'exam_type': 'midterm', 'exam_date': date(2025, 11, 10), 'semester_id': 3},
    {'exam_id': 16, 'exam_name': '2025-2026学年第一学期第二次月考', 'exam_type': 'unit', 'exam_date': date(2025, 12, 5), 'semester_id': 3},
    {'exam_id': 17, 'exam_name': '2025-2026学年第一学期随堂测验二', 'exam_type': 'daily', 'exam_date': date(2025, 12, 19), 'semester_id': 3},
    {'exam_id': 18, 'exam_name': '2025-2026学年第一学期期末考试', 'exam_type': 'final', 'exam_date': date(2026, 1, 9), 'semester_id': 3},
]

# 波动 sigma (按考试类型)
EXAM_SIGMA = {'daily': 0.05, 'unit': 0.04, 'midterm': 0.03, 'final': 0.025}

# 教师配置: (科目名, subject_id, 需要的教师数, 男性比例)
TEACHER_CONFIG = [
    ('语文', 1, 6, 0.30),
    ('数学', 2, 6, 0.60),
    ('英语', 3, 6, 0.30),
    ('物理', 4, 4, 0.60),
    ('化学', 5, 2, 0.60),
    ('道德与法治', 6, 3, 0.50),
    ('历史', 7, 3, 0.50),
    ('地理', 8, 2, 0.50),
    ('生物', 9, 2, 0.50),
]

# ============================================================
# 知识点配置 (科目 -> 年级 -> 知识点树)
# ============================================================
KNOWLEDGE_POINTS = {
    1: {  # 语文
        7: [
            ('阅读与理解', [
                ('记叙文阅读', 2, 5, [('人物描写分析', 2, 4), ('情节概括与理解', 2, 5)]),
                ('说明文阅读', 3, 4, [('说明方法辨析', 3, 4), ('信息提取与归纳', 2, 4)]),
            ]),
            ('基础知识', [
                ('字词积累', 1, 5, [('常用字词读写', 1, 5), ('成语与词义辨析', 2, 4)]),
                ('古诗文默写', 2, 5, [('名篇名句识记', 1, 5)]),
            ]),
            ('写作', [
                ('记叙文写作', 3, 5, [('审题立意', 3, 5), ('结构与表达', 3, 4)]),
            ]),
        ],
        8: [
            ('文言文阅读', [
                ('文言实词虚词', 3, 5, [('常见实词理解', 2, 5), ('虚词用法辨析', 3, 4)]),
                ('文言文翻译', 3, 5, [('直译与意译', 3, 5)]),
            ]),
            ('议论文阅读', [
                ('论点与论据', 3, 4, [('中心论点提取', 2, 5), ('论据类型判断', 3, 4)]),
                ('论证方法', 3, 4, [('举例论证', 2, 4), ('对比论证', 3, 4)]),
            ]),
            ('写作进阶', [
                ('议论文写作', 4, 5, [('论点论据组织', 3, 5), ('议论文结构', 4, 4)]),
            ]),
        ],
        9: [
            ('散文鉴赏', [
                ('散文阅读技巧', 3, 5, [('意象与意境', 3, 4), ('语言赏析', 4, 5)]),
                ('散文主旨把握', 3, 5, [('情感理解', 3, 5)]),
            ]),
            ('名著阅读', [
                ('名著内容理解', 2, 4, [('情节梳理', 2, 4), ('人物形象分析', 3, 5)]),
            ]),
            ('综合写作', [
                ('材料作文', 4, 5, [('材料审题', 4, 5), ('多角度立意', 4, 5)]),
                ('半命题作文', 3, 4, [('补题技巧', 3, 4)]),
            ]),
        ],
    },
    2: {  # 数学
        7: [
            ('有理数', [
                ('有理数的概念', 2, 4, [('正数与负数', 1, 4), ('数轴与绝对值', 2, 5)]),
                ('有理数运算', 2, 5, [('加减运算', 2, 5), ('乘除与混合运算', 3, 5)]),
            ]),
            ('整式', [
                ('整式的概念', 2, 4, [('单项式与多项式', 2, 4)]),
                ('整式的运算', 3, 5, [('合并同类项', 2, 5), ('去括号与添括号', 3, 4)]),
            ]),
            ('一元一次方程', [
                ('方程的解法', 2, 5, [('等式的性质', 2, 4), ('解一元一次方程', 3, 5)]),
                ('方程的应用', 4, 5, [('实际问题建模', 4, 5)]),
            ]),
            ('几何初步', [
                ('线段与角', 2, 4, [('线段的度量', 1, 3), ('角的分类与度量', 2, 4)]),
            ]),
        ],
        8: [
            ('一次函数', [
                ('函数概念', 3, 5, [('自变量与因变量', 2, 4), ('函数图像', 3, 5)]),
                ('一次函数应用', 4, 5, [('函数表达式', 3, 5), ('图像与性质', 3, 5)]),
            ]),
            ('全等三角形', [
                ('全等条件', 3, 5, [('SSS/SAS/ASA/AAS', 3, 5), ('HL判定', 3, 4)]),
                ('全等证明', 4, 5, [('证明思路', 4, 5)]),
            ]),
            ('数据分析', [
                ('统计图表', 2, 4, [('频率分布直方图', 2, 4), ('数据的波动', 3, 4)]),
                ('平均数与方差', 2, 5, [('加权平均数', 3, 5)]),
            ]),
        ],
        9: [
            ('一元二次方程', [
                ('解法', 3, 5, [('公式法', 3, 5), ('因式分解法', 3, 5)]),
                ('判别式与根的关系', 4, 5, [('韦达定理', 4, 5)]),
            ]),
            ('二次函数', [
                ('图像与性质', 4, 5, [('顶点式与一般式', 3, 5), ('开口方向与对称轴', 3, 5)]),
                ('二次函数应用', 5, 5, [('最值问题', 4, 5), ('动态问题', 5, 5)]),
            ]),
            ('圆', [
                ('圆的基本性质', 3, 4, [('圆心角与弧', 3, 4), ('垂径定理', 3, 5)]),
                ('与圆有关的位置关系', 4, 5, [('点与圆', 3, 4), ('直线与圆', 4, 5)]),
            ]),
        ],
    },
    3: {  # 英语
        7: [
            ('词汇与语法', [
                ('基础词汇', 2, 5, [('名词与代词', 1, 5), ('动词时态-一般现在时', 3, 5)]),
                ('句型结构', 2, 4, [('陈述句与疑问句', 2, 4), ('There be句型', 2, 4)]),
            ]),
            ('阅读理解', [
                ('短文理解', 2, 5, [('细节理解', 2, 5), ('主旨大意', 3, 4)]),
            ]),
            ('写作', [
                ('基础写作', 3, 4, [('短文写作', 3, 4), ('书信与日记', 3, 3)]),
            ]),
        ],
        8: [
            ('语法进阶', [
                ('时态', 3, 5, [('过去时与将来时', 3, 5), ('现在完成时', 4, 5)]),
                ('从句入门', 3, 5, [('宾语从句', 3, 5), ('状语从句', 4, 4)]),
            ]),
            ('阅读与完形', [
                ('阅读策略', 3, 5, [('推断题', 3, 5), ('词义猜测', 3, 4)]),
                ('完形填空', 3, 5, [('上下文语境理解', 3, 5)]),
            ]),
            ('写作提升', [
                ('话题作文', 3, 4, [('观点表达', 3, 4), ('连接词运用', 3, 4)]),
            ]),
        ],
        9: [
            ('语法综合', [
                ('复合句', 4, 5, [('定语从句', 4, 5), ('条件状语从句', 4, 5)]),
                ('非谓语动词', 4, 5, [('动名词与不定式', 4, 5)]),
            ]),
            ('阅读综合', [
                ('长文本阅读', 4, 5, [('篇章结构理解', 4, 5), ('观点态度判断', 4, 4)]),
                ('任务型阅读', 3, 4, [('信息匹配', 3, 4)]),
            ]),
            ('综合写作', [
                ('书面表达', 4, 5, [('图表描述', 4, 5), ('议论文写作', 4, 4)]),
            ]),
        ],
    },
    4: {  # 物理
        8: [
            ('声与光', [
                ('声音', 2, 3, [('声音的产生与传播', 1, 3), ('音调响度音色', 2, 3)]),
                ('光学', 3, 4, [('光的反射', 2, 4), ('光的折射与透镜', 3, 5)]),
            ]),
            ('物态变化', [
                ('温度与物态变化', 2, 4, [('熔化与凝固', 2, 4), ('蒸发与沸腾', 2, 4)]),
            ]),
            ('运动与力', [
                ('运动描述', 2, 4, [('速度与路程', 2, 5), ('匀速直线运动', 3, 4)]),
                ('力学基础', 3, 5, [('力的概念', 2, 5), ('牛顿第一定律', 3, 5)]),
            ]),
        ],
        9: [
            ('电学', [
                ('电路基础', 3, 5, [('串联与并联', 2, 5), ('电流电压电阻', 3, 5)]),
                ('欧姆定律', 4, 5, [('欧姆定律应用', 4, 5), ('电阻测量', 3, 4)]),
            ]),
            ('电功与电功率', [
                ('电功率', 4, 5, [('额定功率', 3, 5), ('电功率计算', 4, 5)]),
                ('电热', 3, 4, [('焦耳定律', 3, 4)]),
            ]),
            ('电磁', [
                ('磁现象', 2, 3, [('磁场与磁感线', 2, 3)]),
                ('电磁感应', 3, 4, [('发电机原理', 3, 4)]),
            ]),
        ],
    },
    5: {  # 化学
        9: [
            ('物质的构成', [
                ('分子与原子', 2, 5, [('分子的性质', 2, 4), ('原子结构', 3, 5)]),
                ('元素与化合物', 2, 5, [('元素周期表', 3, 4), ('化学式与化合价', 3, 5)]),
            ]),
            ('化学反应', [
                ('化学方程式', 3, 5, [('质量守恒定律', 3, 5), ('化学方程式配平', 4, 5)]),
                ('反应类型', 3, 4, [('化合与分解', 2, 4), ('置换与复分解', 3, 4)]),
            ]),
            ('常见物质', [
                ('空气与氧气', 2, 4, [('氧气的制备', 2, 4)]),
                ('水与溶液', 3, 4, [('水的净化', 2, 3), ('溶解度与溶液配制', 3, 4)]),
            ]),
        ],
    },
    6: {  # 道德与法治
        7: [
            ('自我成长', [
                ('认识自我', 2, 4, [('自我评价', 1, 4), ('情绪管理', 2, 4)]),
                ('学习方法', 2, 4, [('时间管理', 2, 4)]),
            ]),
            ('交往与沟通', [
                ('友谊', 1, 3, [('交友原则', 1, 3), ('网络交友', 2, 3)]),
            ]),
            ('生命教育', [
                ('珍爱生命', 2, 5, [('生命的独特性', 1, 5), ('安全意识', 2, 5)]),
            ]),
        ],
        8: [
            ('权利义务', [
                ('公民权利', 3, 5, [('基本权利', 2, 5), ('权利行使', 3, 4)]),
                ('公民义务', 3, 5, [('基本义务', 2, 5)]),
            ]),
            ('法律意识', [
                ('法律常识', 2, 4, [('违法与犯罪', 2, 5), ('未成年人保护', 2, 5)]),
            ]),
            ('社会生活', [
                ('社会规则', 2, 4, [('规则与秩序', 2, 4), ('诚信', 2, 4)]),
            ]),
        ],
        9: [
            ('国情教育', [
                ('基本国情', 3, 5, [('社会主义初级阶段', 2, 5), ('基本经济制度', 3, 5)]),
                ('基本国策', 3, 4, [('科教兴国', 2, 4)]),
            ]),
            ('民主与法治', [
                ('民主制度', 3, 4, [('人民代表大会制度', 3, 5), ('基层民主', 3, 4)]),
            ]),
            ('世界与中国', [
                ('全球化', 3, 4, [('开放发展', 2, 4), ('文化多样性', 2, 3)]),
            ]),
        ],
    },
    7: {  # 历史
        7: [
            ('中国古代史-上', [
                ('史前与先秦', 2, 4, [('原始社会', 1, 3), ('夏商西周', 2, 4)]),
                ('秦汉时期', 3, 5, [('秦统一与制度', 2, 5), ('汉朝兴衰', 3, 5)]),
            ]),
            ('中国古代史-下', [
                ('魏晋南北朝', 2, 4, [('三国鼎立', 2, 4), ('民族融合', 2, 3)]),
                ('隋唐时期', 3, 5, [('隋朝统一', 2, 4), ('唐朝繁荣', 3, 5)]),
            ]),
            ('中国古代史-宋元明清', [
                ('宋元时期', 3, 4, [('宋朝经济', 3, 4), ('元朝统治', 2, 4)]),
                ('明清时期', 3, 5, [('明朝制度', 3, 4), ('清朝统治', 3, 5)]),
            ]),
        ],
        8: [
            ('中国近代史', [
                ('鸦片战争与近代化', 3, 5, [('鸦片战争', 2, 5), ('洋务运动', 3, 5)]),
                ('辛亥革命与民国', 3, 5, [('辛亥革命', 3, 5), ('新文化运动', 3, 5)]),
            ]),
            ('中国现代史', [
                ('新中国成立', 3, 5, [('开国大典', 2, 5), ('土地改革', 3, 4)]),
                ('改革开放', 3, 5, [('经济体制改革', 3, 5), ('对外开放', 3, 5)]),
            ]),
            ('世界古代史', [
                ('古代文明', 2, 3, [('四大文明古国', 2, 4), ('古希腊罗马', 2, 4)]),
            ]),
        ],
        9: [
            ('世界近现代史', [
                ('资产阶级革命', 3, 5, [('英国资产阶级革命', 3, 4), ('法国大革命', 3, 5)]),
                ('工业革命', 3, 5, [('第一次工业革命', 3, 5), ('第二次工业革命', 3, 5)]),
            ]),
            ('两次世界大战', [
                ('一战', 3, 4, [('一战的原因与过程', 3, 4)]),
                ('二战', 3, 5, [('二战的原因与过程', 3, 5), ('反法西斯胜利', 3, 5)]),
            ]),
            ('战后世界', [
                ('冷战', 3, 4, [('美苏争霸', 3, 4), ('两极格局', 3, 4)]),
                ('多极化趋势', 3, 4, [('经济全球化', 3, 4)]),
            ]),
        ],
    },
    8: {  # 地理
        7: [
            ('地球与地图', [
                ('地球的运动', 3, 5, [('自转与公转', 2, 5), ('经纬网', 3, 5)]),
                ('地图基础', 2, 4, [('比例尺与方向', 2, 4), ('等高线地形图', 3, 4)]),
            ]),
            ('世界地理', [
                ('大洲与大洋', 2, 4, [('七大洲四大洋', 1, 4), ('海陆分布', 2, 4)]),
                ('世界气候', 3, 5, [('气候类型', 3, 5), ('影响气候的因素', 3, 4)]),
            ]),
            ('自然环境', [
                ('地形与河流', 2, 4, [('五种地形', 2, 4), ('世界主要河流', 2, 3)]),
            ]),
        ],
        8: [
            ('中国地理概况', [
                ('疆域与人口', 2, 5, [('领土与行政区划', 2, 5), ('人口分布', 2, 4)]),
                ('民族与文化', 2, 3, [('民族分布', 2, 3)]),
            ]),
            ('中国自然环境', [
                ('地形地势', 3, 5, [('三级阶梯', 2, 5), ('主要地形区', 3, 4)]),
                ('气候特征', 3, 5, [('季风气候', 3, 5), ('气温与降水', 3, 5)]),
            ]),
            ('中国经济', [
                ('农业', 2, 4, [('种植业分布', 2, 4), ('农业区划', 2, 3)]),
                ('工业与交通', 3, 4, [('工业基地', 3, 4), ('交通网络', 2, 4)]),
            ]),
        ],
    },
    9: {  # 生物
        7: [
            ('生物与生物圈', [
                ('生物的特征', 1, 4, [('生物与非生物', 1, 4), ('生物的基本特征', 1, 4)]),
                ('生态系统', 2, 5, [('生态系统组成', 2, 5), ('食物链与食物网', 2, 5)]),
            ]),
            ('细胞', [
                ('细胞结构', 2, 5, [('动物细胞与植物细胞', 2, 5), ('细胞器功能', 3, 4)]),
                ('细胞分裂', 3, 4, [('有丝分裂', 3, 4)]),
            ]),
            ('植物', [
                ('种子与萌发', 2, 4, [('种子结构', 1, 4), ('萌发条件', 2, 4)]),
                ('光合作用', 3, 5, [('光合作用过程', 3, 5), ('呼吸作用', 3, 5)]),
            ]),
        ],
        8: [
            ('动物', [
                ('无脊椎动物', 2, 3, [('腔肠与扁形动物', 2, 3), ('昆虫', 2, 4)]),
                ('脊椎动物', 2, 4, [('鱼类两栖类', 2, 4), ('鸟类哺乳类', 2, 4)]),
            ]),
            ('人体生理', [
                ('消化系统', 2, 5, [('消化器官', 2, 4), ('营养吸收', 2, 5)]),
                ('循环系统', 3, 5, [('心脏与血液循环', 3, 5), ('血型与输血', 2, 4)]),
            ]),
            ('生物多样性', [
                ('生物分类', 2, 4, [('分类方法', 2, 4), ('生物命名', 2, 3)]),
                ('保护生物多样性', 2, 4, [('濒危物种保护', 2, 4)]),
            ]),
        ],
    },
}

# ============================================================
# 工具函数
# ============================================================

def weighted_choice(items_with_weights):
    """加权随机选择"""
    items, weights = zip(*items_with_weights)
    total = sum(weights)
    r = random.uniform(0, total)
    upto = 0
    for item, w in zip(items, weights):
        upto += w
        if r <= upto:
            return item
    return items[-1]


def generate_phone():
    """生成手机号"""
    prefix = random.choice(PHONE_PREFIXES)
    return prefix + ''.join([str(random.randint(0, 9)) for _ in range(8)])


def bcrypt_hash(password):
    """简化的密码哈希 (用 SHA-256 模拟 BCrypt 格式, 实际部署时应替换为真实 BCrypt)"""
    h = hashlib.sha256(password.encode()).hexdigest()
    return f'$2a$10${h[:53]}'


def sql_str(val):
    """转义 SQL 字符串"""
    if val is None:
        return 'NULL'
    s = str(val).replace("'", "''").replace('\\', '\\\\')
    return f"'{s}'"


def sql_val(val):
    """格式化 SQL 值"""
    if val is None:
        return 'NULL'
    if isinstance(val, bool):
        return 'TRUE' if val else 'FALSE'
    if isinstance(val, (int, float)):
        return str(val)
    if isinstance(val, date):
        return f"'{val.isoformat()}'"
    if isinstance(val, datetime):
        return f"'{val.strftime('%Y-%m-%d %H:%M:%S')}'"
    return sql_str(val)


def clamp(val, lo, hi):
    return max(lo, min(hi, val))


def generate_chinese_name(gender, used_names):
    """生成不重复的中文姓名"""
    pool = MALE_NAMES if gender == 'M' else FEMALE_NAMES
    for _ in range(100):
        surname = weighted_choice(SURNAMES_WEIGHTED)
        given = random.choice(pool)
        name = surname + given
        if name not in used_names:
            used_names.add(name)
            return name
    # 极端情况: 加随机字
    extra = random.choice(['一', '二', '小', '大', '阿'])
    name = weighted_choice(SURNAMES_WEIGHTED) + extra + random.choice(pool)[:1]
    used_names.add(name)
    return name


def is_holiday(d):
    """判断是否为节假日 (简化版)"""
    m, day = d.month, d.day
    # 国庆 10.1-10.7
    if m == 10 and 1 <= day <= 7:
        return True
    # 元旦 1.1
    if m == 1 and day == 1:
        return True
    # 春节 (简化: 1.28-2.4 或 2.10-2.17 取决于年份, 这里用固定范围)
    if d.year == 2025 and m == 1 and 28 <= day <= 31:
        return True
    if d.year == 2025 and m == 2 and 1 <= day <= 4:
        return True
    if d.year == 2026 and m == 2 and 6 <= day <= 12:
        return True
    # 清明 4.4-4.6
    if m == 4 and 4 <= day <= 6:
        return True
    # 五一 5.1-5.5
    if m == 5 and 1 <= day <= 5:
        return True
    # 端午 (简化: 6.10-6.12 for 2025)
    if d.year == 2025 and m == 5 and 31 <= day <= 31:
        return True
    if d.year == 2025 and m == 6 and 1 <= day <= 2:
        return True
    # 中秋 (简化: 9.15-9.17 for 2025)
    if d.year == 2025 and m == 9 and 15 <= day <= 17:
        return True
    return False


# ============================================================
# 数据生成器
# ============================================================

class SeedDataGenerator:
    def __init__(self):
        self.classes = []       # [{class_id, class_name, grade_id, classroom, student_count}]
        self.students = []      # [{student_id, ...}]
        self.teachers = []      # [{teacher_id, ...}]
        self.users = []         # [{user_id, ...}]
        self.assignments = []   # [{assignment_id, ...}]
        self.dates = []         # [{date_id, ...}]
        self.knowledge_points = []  # [{point_id, ...}]
        self.closures = []      # [(parent_id, point_id, distance)]
        self.scores = []        # [{score_id, ...}]
        self.attendances = []   # [{attendance_id, ...}]
        self.profiles = []      # [{profile_id, ...}]
        self.advices = []       # [{advice_id, ...}]

        # 内部追踪
        self.student_ability = {}    # student_id -> {baseline, preference_type, preference_offsets, trend, trend_rate}
        self.class_head_teacher = {}  # class_id -> teacher_id
        self.subject_map = {s['subject_id']: s for s in SUBJECTS}
        self.exam_map = {e['exam_id']: e for e in EXAMS}

        # 知识点索引
        self.kp_by_subject_grade = defaultdict(list)  # (subject_id, grade_level) -> [point_id list (leaf)]

    # ----------------------------------------------------------
    # 1. 班级
    # ----------------------------------------------------------
    def generate_classes(self):
        class_id = 0
        for grade in GRADES:
            for (cname, room) in CLASS_NAMES[grade['grade_id']]:
                class_id += 1
                count = random.randint(38, 42)
                self.classes.append({
                    'class_id': class_id,
                    'class_name': cname,
                    'grade_id': grade['grade_id'],
                    'classroom': room,
                    'student_count': count,
                })
        print(f"  班级: {len(self.classes)} 个")

    # ----------------------------------------------------------
    # 2. 日期维度
    # ----------------------------------------------------------
    def generate_dates(self):
        start = date(2024, 9, 1)
        end = date(2026, 1, 31)
        d = start
        while d <= end:
            # 确定学期
            sem_id = None
            for sem in SEMESTERS:
                if sem['start_date'] <= d <= sem['end_date']:
                    sem_id = sem['semester_id']
                    break

            wd = d.isoweekday()  # 1=Mon ... 7=Sun
            is_school = (wd <= 5 and sem_id is not None and not is_holiday(d))

            self.dates.append({
                'date_id': d.isoformat(),
                'date_value': d,
                'year': d.year,
                'month': d.month,
                'day': d.day,
                'week_day': wd,
                'week_of_year': d.isocalendar()[1],
                'semester_id': sem_id,
                'is_school_day': is_school,
            })
            d += timedelta(days=1)
        school_days = sum(1 for dd in self.dates if dd['is_school_day'])
        print(f"  日期: {len(self.dates)} 天 (其中教学日 {school_days} 天)")

    # ----------------------------------------------------------
    # 3. 教师
    # ----------------------------------------------------------
    def generate_teachers(self):
        teacher_id = 0
        used_names = set()

        # 按科目生成教师, 记录科目归属
        self.teacher_subject = {}  # teacher_id -> subject_id
        self.subject_teachers = defaultdict(list)  # subject_id -> [teacher_id]

        for (subj_name, subj_id, count, male_ratio) in TEACHER_CONFIG:
            for i in range(count):
                teacher_id += 1
                is_male = random.random() < male_ratio
                gender = 'M' if is_male else 'F'
                pool = TEACHER_MALE_NAMES if is_male else TEACHER_FEMALE_NAMES
                # 教师姓名
                for _ in range(50):
                    surname = weighted_choice(SURNAMES_WEIGHTED)
                    given = random.choice(pool)
                    name = surname + given
                    if name not in used_names:
                        used_names.add(name)
                        break

                self.teachers.append({
                    'teacher_id': teacher_id,
                    'teacher_no': f'T{teacher_id:04d}',
                    'teacher_name': name,
                    'gender': gender,
                    'phone': generate_phone(),
                })
                self.teacher_subject[teacher_id] = subj_id
                self.subject_teachers[subj_id].append(teacher_id)

        print(f"  教师: {len(self.teachers)} 名")

    # ----------------------------------------------------------
    # 4. 用户
    # ----------------------------------------------------------
    def generate_users(self):
        uid = 1
        # admin
        self.users.append({
            'user_id': uid,
            'user_name': 'admin',
            'password_hash': bcrypt_hash('admin123'),
            'user_type': 'admin',
            'teacher_id': None,
            'status': 'active',
            'last_login_at': datetime(2026, 2, 20, 9, 30, 0),
        })
        uid += 1

        # 教师账号
        for t in self.teachers:
            login_day = random.randint(15, 22)
            login_hour = random.randint(7, 18)
            self.users.append({
                'user_id': uid,
                'user_name': t['teacher_no'],
                'password_hash': bcrypt_hash('123456'),
                'user_type': 'teacher',
                'teacher_id': t['teacher_id'],
                'status': 'active',
                'last_login_at': datetime(2026, 2, login_day, login_hour, random.randint(0, 59), 0),
            })
            uid += 1

        print(f"  用户: {len(self.users)} 个")

    # ----------------------------------------------------------
    # 5. 教师任课关系
    # ----------------------------------------------------------
    def generate_assignments(self):
        aid = 0
        # 为每个年级的每个科目分配教师到班级
        # 教师按科目分组, 每人带约2-4个班

        # 先确定每个年级-科目需要的班级列表
        grade_classes = defaultdict(list)
        for c in self.classes:
            grade_classes[c['grade_id']].append(c['class_id'])

        # 教师分配策略: 按年级轮流分配同科目的教师
        teacher_class_map = defaultdict(list)  # teacher_id -> [class_id]

        for subj in SUBJECTS:
            sid = subj['subject_id']
            teachers_for_subj = self.subject_teachers[sid]
            # 哪些年级需要这个科目
            relevant_grades = [g for g in [7, 8, 9] if sid in GRADE_SUBJECTS[g]]
            relevant_class_ids = []
            for g in relevant_grades:
                gid = {7: 1, 8: 2, 9: 3}[g]
                relevant_class_ids.extend(grade_classes[gid])

            # 均匀分配
            for i, cid in enumerate(relevant_class_ids):
                tid = teachers_for_subj[i % len(teachers_for_subj)]
                teacher_class_map[tid].append(cid)

                # 为每个学期创建记录
                for sem in SEMESTERS:
                    aid += 1
                    self.assignments.append({
                        'assignment_id': aid,
                        'teacher_id': tid,
                        'class_id': cid,
                        'subject_id': sid,
                        'is_head_teacher': False,
                        'semester_id': sem['semester_id'],
                        'is_current': sem['is_current'],
                    })

        # 班主任分配: 从语数英教师中选, 每班一个
        head_teacher_pool = []
        for sid in [1, 2, 3]:
            head_teacher_pool.extend(self.subject_teachers[sid])

        ht_idx = 0
        for c in self.classes:
            tid = head_teacher_pool[ht_idx % len(head_teacher_pool)]
            ht_idx += 1
            self.class_head_teacher[c['class_id']] = tid

            for sem in SEMESTERS:
                aid += 1
                self.assignments.append({
                    'assignment_id': aid,
                    'teacher_id': tid,
                    'class_id': c['class_id'],
                    'subject_id': None,
                    'is_head_teacher': True,
                    'semester_id': sem['semester_id'],
                    'is_current': sem['is_current'],
                })

        print(f"  任课关系: {len(self.assignments)} 条")

    # ----------------------------------------------------------
    # 6. 学生
    # ----------------------------------------------------------
    def generate_students(self):
        sid = 0
        used_names = set()

        # 预设的特殊学生 (在相应班级中插入)
        preset_students = {
            # class_id: [(name, gender, ability_override)]
            1: [('张浩然', 'M', {'baseline': 0.48, 'pref': 'balanced', 'trend': 'rising', 'trend_rate': 0.015})],
            6: [('李思涵', 'F', {'baseline': 0.82, 'pref': 'extreme_science', 'trend': 'stable', 'trend_rate': 0})],
            11: [('王子轩', 'M', {'baseline': 0.93, 'pref': 'balanced', 'trend': 'stable', 'trend_rate': 0})],
            8: [('刘天宇', 'M', {'baseline': 0.78, 'pref': 'balanced', 'trend': 'declining', 'trend_rate': -0.012})],
            3: [('赵小燕', 'F', {'baseline': 0.62, 'pref': 'arts', 'trend': 'stable', 'trend_rate': 0, 'sick_streak': True})],
        }

        # 预先占位所有预设学生姓名, 防止随机生成重名
        for presets_list in preset_students.values():
            for (pname, _, _) in presets_list:
                used_names.add(pname)

        for c in self.classes:
            grade = next(g for g in GRADES if g['grade_id'] == c['grade_id'])
            gl = grade['grade_level']
            target_count = c['student_count']
            enroll_year = 2024 - (gl - 7)

            # 出生年份范围
            birth_year_start = enroll_year - 13
            birth_year_end = enroll_year - 12

            presets = preset_students.get(c['class_id'], [])
            preset_names = {p[0] for p in presets}

            for i in range(target_count):
                sid += 1

                # 检查是否有预设学生
                preset = None
                if i < len(presets):
                    preset = presets[i]

                if preset:
                    name = preset[0]
                    gender = preset[1]
                    used_names.add(name)
                else:
                    # 性别: 52% 男
                    gender = 'M' if random.random() < 0.52 else 'F'
                    name = generate_chinese_name(gender, used_names)

                # 学号
                class_seq = (c['class_id'] - 1) % 4 + 1
                student_no = f'{enroll_year}{class_seq:02d}{(i + 1):02d}'

                # 出生日期
                bd = date(random.randint(birth_year_start, birth_year_end),
                          random.randint(1, 12),
                          random.randint(1, 28))

                # 学籍状态
                status = 'active'
                if not preset and random.random() < 0.01:
                    status = 'transferred'

                student = {
                    'student_id': sid,
                    'student_no': student_no,
                    'student_name': name,
                    'gender': gender,
                    'birth_date': bd,
                    'class_id': c['class_id'],
                    'grade_id': c['grade_id'],
                    'grade_level': gl,
                    'enroll_date': date(enroll_year, 9, 1),
                    'phone': generate_phone() if random.random() < 0.6 else None,
                    'student_status': status,
                }
                self.students.append(student)

                # 能力模型
                if preset and preset[2]:
                    ov = preset[2]
                    ability = self._build_ability(ov['baseline'], ov['pref'], ov['trend'], ov['trend_rate'])
                    if ov.get('sick_streak'):
                        ability['sick_streak'] = True
                else:
                    ability = self._random_ability()

                self.student_ability[sid] = ability

        # 更新 class.student_count 为真实值
        class_counts = defaultdict(int)
        for s in self.students:
            class_counts[s['class_id']] += 1
        for c in self.classes:
            c['student_count'] = class_counts[c['class_id']]

        print(f"  学生: {len(self.students)} 名")

    def _random_ability(self):
        """随机生成学生能力模型"""
        r = random.random()
        if r < 0.15:
            baseline = random.uniform(0.85, 0.95)
        elif r < 0.45:
            baseline = random.uniform(0.72, 0.85)
        elif r < 0.80:
            baseline = random.uniform(0.58, 0.72)
        else:
            baseline = random.uniform(0.40, 0.58)

        # 科目偏好
        r2 = random.random()
        if r2 < 0.25:
            pref = 'arts'
        elif r2 < 0.50:
            pref = 'science'
        elif r2 < 0.90:
            pref = 'balanced'
        else:
            pref = 'extreme'

        # 成绩趋势
        r3 = random.random()
        if r3 < 0.25:
            trend = 'rising'
            trend_rate = random.uniform(0.005, 0.015)
        elif r3 < 0.75:
            trend = 'stable'
            trend_rate = 0
        elif r3 < 0.90:
            trend = 'declining'
            trend_rate = random.uniform(-0.012, -0.005)
        else:
            trend = 'wave'
            trend_rate = random.uniform(0.02, 0.03)

        return self._build_ability(baseline, pref, trend, trend_rate)

    def _build_ability(self, baseline, pref, trend, trend_rate):
        """构建能力模型"""
        # 科目偏好偏移量 {subject_id: offset}
        offsets = {}
        arts_subjects = [1, 3, 6, 7]      # 语文, 英语, 道法, 历史
        science_subjects = [2, 4, 5]       # 数学, 物理, 化学
        neutral_subjects = [8, 9]           # 地理, 生物

        if pref == 'arts':
            for sid in arts_subjects:
                offsets[sid] = random.uniform(0.03, 0.08)
            for sid in science_subjects:
                offsets[sid] = random.uniform(-0.08, -0.03)
            for sid in neutral_subjects:
                offsets[sid] = random.uniform(-0.01, 0.01)
        elif pref == 'science':
            for sid in science_subjects:
                offsets[sid] = random.uniform(0.03, 0.08)
            for sid in arts_subjects:
                offsets[sid] = random.uniform(-0.05, -0.03)
            for sid in neutral_subjects:
                offsets[sid] = random.uniform(-0.01, 0.01)
        elif pref == 'extreme' or pref == 'extreme_science':
            # 严重偏科
            strong = random.sample(science_subjects, min(2, len(science_subjects))) if pref == 'extreme_science' else random.sample(range(1, 10), 2)
            weak = random.sample([s for s in range(1, 10) if s not in strong], 2)
            for sid in range(1, 10):
                if sid in strong:
                    offsets[sid] = random.uniform(0.08, 0.12)
                elif sid in weak:
                    offsets[sid] = random.uniform(-0.15, -0.08)
                else:
                    offsets[sid] = random.uniform(-0.02, 0.02)
            # 特殊: 李思涵 数学物理超强, 英语很弱
            if pref == 'extreme_science':
                offsets[2] = 0.12   # 数学
                offsets[4] = 0.10   # 物理
                offsets[3] = -0.18  # 英语
        else:  # balanced
            for sid in range(1, 10):
                offsets[sid] = random.uniform(-0.02, 0.02)

        return {
            'baseline': baseline,
            'pref': pref,
            'offsets': offsets,
            'trend': trend,
            'trend_rate': trend_rate,
        }

    # ----------------------------------------------------------
    # 7. 知识点
    # ----------------------------------------------------------
    def generate_knowledge_points(self):
        pid = 0

        for subj_id, grades_data in KNOWLEDGE_POINTS.items():
            for grade_level, categories in grades_data.items():
                grade_id = {7: 1, 8: 2, 9: 3}.get(grade_level)
                subj = self.subject_map[subj_id]
                subj_code = {1: 'YW', 2: 'MATH', 3: 'EN', 4: 'PHY', 5: 'CHEM',
                             6: 'DF', 7: 'LS', 8: 'DL', 9: 'SW'}[subj_id]

                for cat_idx, (cat_name, children) in enumerate(categories, 1):
                    # 一级知识点
                    pid += 1
                    cat_pid = pid
                    cat_code = f'{subj_code}-{grade_level}-{cat_idx}'
                    self.knowledge_points.append({
                        'point_id': pid,
                        'point_code': cat_code,
                        'point_name': cat_name,
                        'subject_id': subj_id,
                        'grade_id': grade_id,
                        'parent_id': None,
                        'point_level': 1,
                        'difficulty': None,
                        'importance': None,
                        'description': None,
                    })
                    self.closures.append((pid, pid, 0))

                    for ch_idx, (ch_name, ch_diff, ch_imp, leaves) in enumerate(children, 1):
                        # 二级知识点
                        pid += 1
                        ch_pid = pid
                        ch_code = f'{cat_code}.{ch_idx}'
                        self.knowledge_points.append({
                            'point_id': pid,
                            'point_code': ch_code,
                            'point_name': ch_name,
                            'subject_id': subj_id,
                            'grade_id': grade_id,
                            'parent_id': cat_pid,
                            'point_level': 2,
                            'difficulty': ch_diff,
                            'importance': ch_imp,
                            'description': None,
                        })
                        self.closures.append((pid, pid, 0))
                        self.closures.append((cat_pid, pid, 1))

                        for leaf_idx, leaf_data in enumerate(leaves, 1):
                            leaf_name = leaf_data[0]
                            leaf_diff = leaf_data[1]
                            leaf_imp = leaf_data[2]
                            # 三级知识点
                            pid += 1
                            leaf_code = f'{ch_code}.{leaf_idx}'
                            self.knowledge_points.append({
                                'point_id': pid,
                                'point_code': leaf_code,
                                'point_name': leaf_name,
                                'subject_id': subj_id,
                                'grade_id': grade_id,
                                'parent_id': ch_pid,
                                'point_level': 3,
                                'difficulty': leaf_diff,
                                'importance': leaf_imp,
                                'description': None,
                            })
                            self.closures.append((pid, pid, 0))
                            self.closures.append((ch_pid, pid, 1))
                            self.closures.append((cat_pid, pid, 2))

                            # 记录叶子节点, 方便后续学习建议关联
                            self.kp_by_subject_grade[(subj_id, grade_level)].append(pid)

        print(f"  知识点: {len(self.knowledge_points)} 个")
        print(f"  闭包关系: {len(self.closures)} 条")

    # ----------------------------------------------------------
    # 8. 成绩
    # ----------------------------------------------------------
    def generate_scores(self):
        score_id = 0
        # 按考试顺序排列, 方便计算趋势偏移
        sorted_exams = sorted(EXAMS, key=lambda e: e['exam_date'])
        exam_order = {e['exam_id']: i for i, e in enumerate(sorted_exams)}

        for student in self.students:
            if student['student_status'] == 'transferred':
                # 转学生: 第一学期有成绩, 后续无
                last_exam_id = 6  # 第一学期期末
            else:
                last_exam_id = 999

            gl = student['grade_level']

            for exam in sorted_exams:
                if exam['exam_id'] > last_exam_id:
                    break

                # 确定该考试考哪些科目
                if exam['exam_type'] in ('daily', 'unit'):
                    subject_ids = MAIN_SUBJECTS[gl]
                else:
                    subject_ids = GRADE_SUBJECTS[gl]

                # 0.5% 概率缺考
                if random.random() < 0.005:
                    continue

                ability = self.student_ability[student['student_id']]
                exam_idx = exam_order[exam['exam_id']]

                for subj_id in subject_ids:
                    subj = self.subject_map[subj_id]
                    full = subj['full_score']

                    # 计算得分率
                    base = ability['baseline']
                    offset = ability['offsets'].get(subj_id, 0)

                    # 趋势偏移
                    trend_offset = 0
                    if ability['trend'] == 'rising':
                        trend_offset = ability['trend_rate'] * exam_idx
                        trend_offset = min(trend_offset, 0.10)
                    elif ability['trend'] == 'declining':
                        trend_offset = ability['trend_rate'] * exam_idx
                        trend_offset = max(trend_offset, -0.08)
                    elif ability['trend'] == 'wave':
                        trend_offset = ability['trend_rate'] * math.sin(exam_idx * 0.8)

                    # 随机波动
                    sigma = EXAM_SIGMA[exam['exam_type']]
                    noise = random.gauss(0, sigma)

                    score_rate = base + offset + trend_offset + noise
                    score_rate = clamp(score_rate, 0.10, 1.0)

                    score = round(score_rate * full, 1)
                    score = clamp(score, full * 0.05, full)

                    # score_level
                    if score >= subj['excellent_score']:
                        level = 'A'
                    elif score >= subj['pass_score']:
                        level = 'B'
                    elif score >= full * 0.40:
                        level = 'C'
                    else:
                        level = 'D'

                    score_id += 1
                    self.scores.append({
                        'score_id': score_id,
                        'student_id': student['student_id'],
                        'class_id': student['class_id'],
                        'subject_id': subj_id,
                        'exam_id': exam['exam_id'],
                        'score': score,
                        'score_level': level,
                    })

        print(f"  成绩: {len(self.scores)} 条")

    # ----------------------------------------------------------
    # 9. 考勤 (仅存异常)
    # ----------------------------------------------------------
    def generate_attendance(self):
        att_id = 0

        school_days = [d for d in self.dates if d['is_school_day']]

        # 标记习惯性迟到学生
        habitual_late = set()
        for grade_id in [1, 2, 3]:
            grade_students = [s for s in self.students
                              if s['grade_id'] == grade_id
                              and s['student_status'] == 'active'
                              and self.student_ability[s['student_id']]['baseline'] < 0.55]
            if grade_students:
                for s in random.sample(grade_students, min(3, len(grade_students))):
                    habitual_late.add(s['student_id'])

        # 标记连续病假学生
        sick_streak_students = set()
        for s in self.students:
            if self.student_ability[s['student_id']].get('sick_streak'):
                sick_streak_students.add(s['student_id'])

        # 为每个学期选几个连续病假学生
        for sem in SEMESTERS:
            sem_students = [s for s in self.students
                            if s['student_status'] == 'active' and s['student_id'] not in sick_streak_students]
            for s in random.sample(sem_students, min(4, len(sem_students))):
                sick_streak_students.add(s['student_id'])

        # 为连续病假学生生成病假时间段
        sick_periods = {}  # student_id -> [(start_date, duration)]
        for sid_val in sick_streak_students:
            periods = []
            for sem in SEMESTERS:
                if random.random() < 0.5:
                    sem_school_days = [d for d in school_days
                                       if sem['start_date'] <= d['date_value'] <= sem['end_date']]
                    if len(sem_school_days) > 10:
                        start_idx = random.randint(5, len(sem_school_days) - 6)
                        duration = random.randint(3, 5)
                        start_d = sem_school_days[start_idx]['date_value']
                        periods.append((start_d, duration))
            sick_periods[sid_val] = periods

        for student in self.students:
            if student['student_status'] == 'transferred':
                continue

            stid = student['student_id']
            is_habitual = stid in habitual_late

            for day_info in school_days:
                d = day_info['date_value']
                is_monday = day_info['week_day'] == 1
                is_winter = day_info['month'] in [11, 12, 1]

                for time_slot in ['morning', 'afternoon']:
                    # 检查是否在连续病假期间
                    in_sick_period = False
                    if stid in sick_periods:
                        for (start_d, dur) in sick_periods[stid]:
                            if start_d <= d < start_d + timedelta(days=dur + 2):
                                in_sick_period = True
                                break

                    if in_sick_period:
                        status = 'sick_leave'
                        reason = random.choice(SICK_REASONS)
                    else:
                        # 正常异常率计算
                        late_rate = 0.008 if not is_habitual else 0.04
                        if is_monday:
                            late_rate *= 1.5
                        sick_rate = 0.003
                        if is_winter:
                            sick_rate *= 1.8
                        absent_rate = 0.002
                        leave_early_rate = 0.002

                        r = random.random()
                        if r < late_rate and time_slot == 'morning':
                            status = 'late'
                            reason = random.choice(LATE_REASONS)
                        elif r < late_rate + sick_rate:
                            status = 'sick_leave'
                            reason = random.choice(SICK_REASONS)
                        elif r < late_rate + sick_rate + absent_rate:
                            status = 'absent'
                            reason = random.choice(ABSENT_REASONS)
                        elif r < late_rate + sick_rate + absent_rate + leave_early_rate and time_slot == 'afternoon':
                            status = 'leave_early'
                            reason = random.choice(LEAVE_EARLY_REASONS)
                        else:
                            continue  # 正常出勤, 不记录

                    att_id += 1
                    recorded_by = self.class_head_teacher.get(student['class_id'])
                    self.attendances.append({
                        'attendance_id': att_id,
                        'student_id': stid,
                        'class_id': student['class_id'],
                        'date_id': day_info['date_id'],
                        'status': status,
                        'time_slot': time_slot,
                        'reason': reason,
                        'recorded_by': recorded_by,
                    })

        print(f"  考勤异常: {len(self.attendances)} 条")

    # ----------------------------------------------------------
    # 10. 学生画像
    # ----------------------------------------------------------
    def generate_profiles(self):
        import json

        # 计算每个学生最近一次期末的各科平均得分率
        student_final_scores = defaultdict(list)  # student_id -> [score_rate, ...]
        for sc in self.scores:
            exam = self.exam_map[sc['exam_id']]
            if exam['exam_type'] == 'final' and exam['exam_id'] == 18:  # 最近期末
                subj = self.subject_map[sc['subject_id']]
                student_final_scores[sc['student_id']].append(sc['score'] / subj['full_score'])

        # 计算趋势 (基于 3 次大考的平均分)
        major_exam_ids = [eid for eid, ex in self.exam_map.items() if ex['exam_type'] in ('midterm', 'final')]
        major_exam_ids.sort(key=lambda x: self.exam_map[x]['exam_date'])

        student_major_avgs = defaultdict(list)
        for sc in self.scores:
            if sc['exam_id'] in major_exam_ids:
                subj = self.subject_map[sc['subject_id']]
                student_major_avgs[sc['student_id']].append((sc['exam_id'], sc['score'] / subj['full_score']))

        # 学生科目得分率 (最近学期)
        student_subj_rates = defaultdict(lambda: defaultdict(list))
        recent_exams = [e for e in EXAMS if e['semester_id'] == 3]
        recent_exam_ids = {e['exam_id'] for e in recent_exams}
        for sc in self.scores:
            if sc['exam_id'] in recent_exam_ids:
                subj = self.subject_map[sc['subject_id']]
                student_subj_rates[sc['student_id']][sc['subject_id']].append(sc['score'] / subj['full_score'])

        for pid_idx, student in enumerate(self.students, 1):
            stid = student['student_id']
            ability = self.student_ability[stid]

            # overall_level
            final_rates = student_final_scores.get(stid, [])
            if final_rates:
                avg_rate = sum(final_rates) / len(final_rates)
            else:
                avg_rate = ability['baseline']

            if avg_rate >= 0.85:
                level = 'A'
            elif avg_rate >= 0.60:
                level = 'B'
            elif avg_rate >= 0.40:
                level = 'C'
            else:
                level = 'D'

            # score_trend
            trend_label = ability['trend']
            if trend_label == 'wave':
                trend_label = 'stable'

            # strength/weakness subjects
            subj_rates = student_subj_rates.get(stid, {})
            subj_avg = {}
            for sid, rates in subj_rates.items():
                subj_avg[sid] = sum(rates) / len(rates)

            gl = student['grade_level']
            available_subjects = GRADE_SUBJECTS[gl]
            sorted_subjs = sorted(
                [(sid, subj_avg.get(sid, ability['baseline'])) for sid in available_subjects],
                key=lambda x: x[1], reverse=True
            )

            strength_subjs = [self.subject_map[s[0]]['subject_name'] for s in sorted_subjs[:2]]
            weakness_subjs = [self.subject_map[s[0]]['subject_name'] for s in sorted_subjs[-2:]]

            # 知识点 (从强/弱科目中选)
            strength_points = []
            weakness_points = []
            if sorted_subjs:
                s_sid = sorted_subjs[0][0]
                w_sid = sorted_subjs[-1][0]
                s_kps = self.kp_by_subject_grade.get((s_sid, gl), [])
                w_kps = self.kp_by_subject_grade.get((w_sid, gl), [])
                if s_kps:
                    sp_ids = random.sample(s_kps, min(2, len(s_kps)))
                    strength_points = [next(k['point_name'] for k in self.knowledge_points if k['point_id'] == pid_v) for pid_v in sp_ids]
                if w_kps:
                    wp_ids = random.sample(w_kps, min(2, len(w_kps)))
                    weakness_points = [next(k['point_name'] for k in self.knowledge_points if k['point_id'] == pid_v) for pid_v in wp_ids]

            # learning_style
            pref = ability['pref']
            style_map = {'science': '逻辑思维型', 'extreme_science': '逻辑思维型',
                         'arts': '记忆积累型', 'extreme': '实践操作型', 'balanced': '综合均衡型'}
            learning_style = style_map.get(pref, '综合均衡型')

            # AI 摘要
            level_desc = {'A': '优秀', 'B': '良好', 'C': '中等', 'D': '待加强'}
            attitude_desc = {'A': '积极主动', 'B': '较为认真', 'C': '有待改进', 'D': '需要更多关注'}
            trend_cn = {'rising': '上升趋势', 'stable': '基本稳定', 'declining': '下降趋势'}

            if level == 'A':
                assessment = f"该生学业表现优秀，学习主动性强，基础扎实。在{'/'.join(strength_subjs)}方面表现突出，成绩{trend_cn[trend_label]}。建议在保持优势的同时，适当拓展更高难度的挑战性内容。"
            elif level == 'B':
                assessment = f"该生学业表现良好，学习态度较为认真。{'/'.join(strength_subjs)}成绩稳定，但{'/'.join(weakness_subjs)}仍有提升空间。成绩{trend_cn[trend_label]}，建议加强弱势科目的专项练习。"
            elif level == 'C':
                assessment = f"该生学业表现中等，学习态度尚可但缺乏主动性。{'/'.join(weakness_subjs)}存在明显薄弱环节，需要重点关注。成绩{trend_cn[trend_label]}，建议制定针对性的复习计划。"
            else:
                assessment = f"该生学业基础薄弱，多科成绩不理想。{'/'.join(weakness_subjs)}急需加强辅导。成绩{trend_cn[trend_label]}，建议从基础知识开始补漏，老师和家长需给予更多关注和鼓励。"

            risk_alerts = []
            if trend_label == 'declining':
                risk_alerts.append(f'近期成绩呈下降趋势，{weakness_subjs[0]}下降明显')
            if stid in {s['student_id'] for s in self.students if self.student_ability[s['student_id']].get('sick_streak')}:
                risk_alerts.append('本学期有连续缺勤记录，请关注')

            ai_summary = json.dumps({
                'overall_assessment': assessment,
                'strength_subjects': strength_subjs,
                'weakness_subjects': weakness_subjs,
                'strength_points': strength_points,
                'weakness_points': weakness_points,
                'learning_style': learning_style,
                'risk_alerts': risk_alerts,
            }, ensure_ascii=False)

            self.profiles.append({
                'profile_id': pid_idx,
                'student_id': stid,
                'overall_level': level,
                'score_trend': trend_label,
                'ai_summary': ai_summary,
                'refreshed_at': datetime(2026, 1, 12, 3, 0, 0),
            })

        print(f"  学生画像: {len(self.profiles)} 条")

    # ----------------------------------------------------------
    # 11. 学习建议
    # ----------------------------------------------------------
    def generate_advices(self):
        adv_id = 0

        advice_templates = {
            'review': [
                '建议回顾{point}的基础概念，重新学习课本相关内容，确保理解核心原理。',
                '{point}掌握不牢固，建议重新梳理知识框架，配合课本例题复习。',
                '针对{point}的薄弱环节，建议每天花15分钟回顾相关知识点。',
            ],
            'practice': [
                '建议针对{point}进行专项练习，每天完成2-3道相关题目。',
                '{point}需要通过大量练习来巩固，建议每周完成一套专项训练。',
                '在{point}方面需要加强练习，建议从基础题开始，逐步提高难度。',
            ],
            'consolidate': [
                '{point}的掌握程度尚可，建议通过综合题型巩固，将知识融会贯通。',
                '在{point}方面已有一定基础，建议多做综合性练习题来巩固提升。',
            ],
            'extend': [
                '在{point}方面已有较好基础，建议挑战更高难度的题目，尝试竞赛题。',
                '{point}掌握优秀，建议拓展延伸阅读，探索更深层次的知识。',
            ],
        }

        feedback_templates = [
            '学生已完成相关练习，掌握情况有所改善',
            '已布置额外作业，学生认真完成',
            '经过一周的专项训练，该知识点已基本掌握',
            '学生进步明显，继续保持',
        ]

        for student in self.students:
            stid = student['student_id']
            ability = self.student_ability[stid]
            gl = student['grade_level']

            # 根据能力层级决定建议数量和类型
            baseline = ability['baseline']
            if baseline >= 0.85:
                num = 2
                types = [('extend', 'low'), ('consolidate', 'low')]
            elif baseline >= 0.72:
                num = 3
                types = [('consolidate', 'medium'), ('practice', 'medium'), ('extend', 'low')]
            elif baseline >= 0.58:
                num = 4
                types = [('practice', 'medium'), ('practice', 'high'), ('review', 'high'), ('consolidate', 'medium')]
            else:
                num = 5
                types = [('review', 'high'), ('review', 'high'), ('practice', 'high'), ('practice', 'medium'), ('review', 'medium')]

            # 选择弱势科目的知识点
            offsets = ability['offsets']
            available = [sid for sid in GRADE_SUBJECTS[gl]]
            sorted_by_weakness = sorted(available, key=lambda s: offsets.get(s, 0))

            for i in range(min(num, len(types))):
                adv_type, adv_level = types[i]

                # 选科目 (弱势优先, extend 选强势)
                if adv_type == 'extend':
                    subj_id = sorted_by_weakness[-1] if sorted_by_weakness else available[0]
                else:
                    subj_id = sorted_by_weakness[i % len(sorted_by_weakness)]

                # 选知识点
                kps = self.kp_by_subject_grade.get((subj_id, gl), [])
                point_id = random.choice(kps) if kps else None
                point_name = ''
                if point_id:
                    point_name = next((k['point_name'] for k in self.knowledge_points if k['point_id'] == point_id), '')

                template = random.choice(advice_templates[adv_type])
                content = template.format(point=point_name) if point_name else template.format(point='相关知识点')

                # status
                r = random.random()
                if r < 0.60:
                    status = 'pending'
                elif r < 0.90:
                    status = 'done'
                else:
                    status = 'ignored'

                feedback = None
                if status == 'done' and random.random() < 0.5:
                    feedback = random.choice(feedback_templates)

                gen_type = 'ai' if random.random() < 0.3 else 'rule'

                adv_id += 1
                self.advices.append({
                    'advice_id': adv_id,
                    'student_id': stid,
                    'subject_id': subj_id,
                    'point_id': point_id,
                    'advice_type': adv_type,
                    'advice_level': adv_level,
                    'advice_content': content,
                    'generate_type': gen_type,
                    'status': status,
                    'feedback': feedback,
                    'created_at': datetime(2026, 1, 12, random.randint(0, 23), random.randint(0, 59), 0),
                    'expires_at': datetime(2026, 2, 11, 23, 59, 59),
                })

        print(f"  学习建议: {len(self.advices)} 条")

    # ----------------------------------------------------------
    # SQL 输出
    # ----------------------------------------------------------
    def write_sql(self, filepath):
        print(f"\n正在写入 SQL 文件: {filepath}")
        os.makedirs(os.path.dirname(filepath), exist_ok=True)

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('-- ============================================================\n')
            f.write('-- Student Analytics - 示例数据 (明德初级中学)\n')
            f.write(f'-- 生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}\n')
            f.write('-- 随机种子: 42 (可复现)\n')
            f.write('-- ============================================================\n\n')
            f.write('SET NAMES utf8mb4;\n')
            f.write('SET FOREIGN_KEY_CHECKS = 0;\n')
            f.write('USE student_analytics;\n\n')

            # 清空数据 (按反向依赖顺序)
            tables_to_truncate = [
                'fact_learning_advice', 'agg_student_profile', 'fact_attendance',
                'fact_score', 'knowledge_point_closure', 'dim_knowledge_point',
                'fact_teacher_assignment', 'dim_exam', 'dim_student', 'dim_user',
                'dim_teacher', 'dim_subject', 'dim_date', 'dim_semester',
                'dim_class', 'dim_grade',
            ]
            for t in tables_to_truncate:
                f.write(f'TRUNCATE TABLE {t};\n')
            f.write('\n')

            # 1. dim_grade
            self._write_inserts(f, 'dim_grade',
                ['grade_id', 'grade_name', 'grade_level', 'stage'],
                [(g['grade_id'], g['grade_name'], g['grade_level'], g['stage']) for g in GRADES])

            # 2. dim_class
            self._write_inserts(f, 'dim_class',
                ['class_id', 'class_name', 'grade_id', 'classroom', 'student_count'],
                [(c['class_id'], c['class_name'], c['grade_id'], c['classroom'], c['student_count']) for c in self.classes])

            # 3. dim_semester
            self._write_inserts(f, 'dim_semester',
                ['semester_id', 'semester_name', 'school_year', 'semester_type', 'start_date', 'end_date', 'is_current'],
                [(s['semester_id'], s['semester_name'], s['school_year'], s['semester_type'],
                  s['start_date'], s['end_date'], s['is_current']) for s in SEMESTERS])

            # 4. dim_date
            self._write_inserts(f, 'dim_date',
                ['date_id', 'date_value', 'year', 'month', 'day', 'week_day', 'week_of_year', 'semester_id', 'is_school_day'],
                [(d['date_id'], d['date_value'], d['year'], d['month'], d['day'],
                  d['week_day'], d['week_of_year'], d['semester_id'], d['is_school_day']) for d in self.dates])

            # 5. dim_subject
            self._write_inserts(f, 'dim_subject',
                ['subject_id', 'subject_name', 'subject_type', 'full_score', 'pass_score', 'excellent_score', 'is_exam_subject', 'sort_order'],
                [(s['subject_id'], s['subject_name'], s['subject_type'], s['full_score'],
                  s['pass_score'], s['excellent_score'], s['is_exam_subject'], s['sort_order']) for s in SUBJECTS])

            # 6. dim_teacher
            self._write_inserts(f, 'dim_teacher',
                ['teacher_id', 'teacher_no', 'teacher_name', 'gender', 'phone'],
                [(t['teacher_id'], t['teacher_no'], t['teacher_name'], t['gender'], t['phone']) for t in self.teachers])

            # 7. dim_user
            self._write_inserts(f, 'dim_user',
                ['user_id', 'user_name', 'password_hash', 'user_type', 'teacher_id', 'status', 'last_login_at'],
                [(u['user_id'], u['user_name'], u['password_hash'], u['user_type'],
                  u['teacher_id'], u['status'], u['last_login_at']) for u in self.users])

            # 8. dim_student
            self._write_inserts(f, 'dim_student',
                ['student_id', 'student_no', 'student_name', 'gender', 'birth_date', 'class_id', 'enroll_date', 'phone', 'student_status'],
                [(s['student_id'], s['student_no'], s['student_name'], s['gender'], s['birth_date'],
                  s['class_id'], s['enroll_date'], s['phone'], s['student_status']) for s in self.students])

            # 9. dim_exam
            self._write_inserts(f, 'dim_exam',
                ['exam_id', 'exam_name', 'exam_type', 'exam_date', 'semester_id'],
                [(e['exam_id'], e['exam_name'], e['exam_type'], e['exam_date'], e['semester_id']) for e in EXAMS])

            # 10. dim_knowledge_point
            self._write_inserts(f, 'dim_knowledge_point',
                ['point_id', 'point_code', 'point_name', 'subject_id', 'grade_id', 'parent_id', 'point_level', 'difficulty', 'importance', 'description'],
                [(k['point_id'], k['point_code'], k['point_name'], k['subject_id'], k['grade_id'],
                  k['parent_id'], k['point_level'], k['difficulty'], k['importance'], k['description']) for k in self.knowledge_points])

            # 11. knowledge_point_closure
            self._write_inserts(f, 'knowledge_point_closure',
                ['parent_id', 'point_id', 'distance'],
                self.closures)

            # 12. fact_teacher_assignment
            self._write_inserts(f, 'fact_teacher_assignment',
                ['assignment_id', 'teacher_id', 'class_id', 'subject_id', 'is_head_teacher', 'semester_id', 'is_current'],
                [(a['assignment_id'], a['teacher_id'], a['class_id'], a['subject_id'],
                  a['is_head_teacher'], a['semester_id'], a['is_current']) for a in self.assignments])

            # 13. fact_score (大表, 分批写)
            self._write_inserts(f, 'fact_score',
                ['score_id', 'student_id', 'class_id', 'subject_id', 'exam_id', 'score', 'score_level'],
                [(s['score_id'], s['student_id'], s['class_id'], s['subject_id'],
                  s['exam_id'], s['score'], s['score_level']) for s in self.scores])

            # 14. fact_attendance
            self._write_inserts(f, 'fact_attendance',
                ['attendance_id', 'student_id', 'class_id', 'date_id', 'status', 'time_slot', 'reason', 'recorded_by'],
                [(a['attendance_id'], a['student_id'], a['class_id'], a['date_id'],
                  a['status'], a['time_slot'], a['reason'], a['recorded_by']) for a in self.attendances])

            # 15. agg_student_profile
            self._write_inserts(f, 'agg_student_profile',
                ['profile_id', 'student_id', 'overall_level', 'score_trend', 'ai_summary', 'refreshed_at'],
                [(p['profile_id'], p['student_id'], p['overall_level'], p['score_trend'],
                  p['ai_summary'], p['refreshed_at']) for p in self.profiles])

            # 16. fact_learning_advice
            self._write_inserts(f, 'fact_learning_advice',
                ['advice_id', 'student_id', 'subject_id', 'point_id', 'advice_type', 'advice_level',
                 'advice_content', 'generate_type', 'status', 'feedback', 'created_at', 'expires_at'],
                [(a['advice_id'], a['student_id'], a['subject_id'], a['point_id'], a['advice_type'],
                  a['advice_level'], a['advice_content'], a['generate_type'], a['status'],
                  a['feedback'], a['created_at'], a['expires_at']) for a in self.advices])

            f.write('\nSET FOREIGN_KEY_CHECKS = 1;\n')
            f.write('-- 数据导入完成\n')

        file_size = os.path.getsize(filepath)
        print(f"SQL file generated: {filepath}")
        print(f"File size: {file_size / 1024 / 1024:.1f} MB")

    def _write_inserts(self, f, table, columns, rows):
        """批量写入 INSERT 语句"""
        if not rows:
            return

        f.write(f'-- {table} ({len(rows)} rows)\n')
        col_str = ', '.join(columns)

        for batch_start in range(0, len(rows), BATCH_SIZE):
            batch = rows[batch_start:batch_start + BATCH_SIZE]
            f.write(f'INSERT INTO {table} ({col_str}) VALUES\n')

            for i, row in enumerate(batch):
                values = ', '.join(sql_val(v) for v in row)
                separator = ',' if i < len(batch) - 1 else ';'
                f.write(f'  ({values}){separator}\n')

            f.write('\n')

    # ----------------------------------------------------------
    # 主流程
    # ----------------------------------------------------------
    def run(self):
        print('[Student Analytics] - seed data generator')
        print('=' * 50)
        print('school: Ming De Junior High')
        print('scale: 3 grades x 4 classes x ~40 students')
        print('span:  3 semesters')
        print('=' * 50)
        print()
        print('Generating data...')

        self.generate_classes()
        self.generate_dates()
        self.generate_teachers()
        self.generate_users()
        self.generate_assignments()
        self.generate_students()
        self.generate_knowledge_points()
        self.generate_scores()
        self.generate_attendance()
        self.generate_profiles()
        self.generate_advices()

        total = (len(GRADES) + len(self.classes) + len(SEMESTERS) + len(self.dates) +
                 len(SUBJECTS) + len(self.teachers) + len(self.users) + len(self.students) +
                 len(EXAMS) + len(self.knowledge_points) + len(self.closures) +
                 len(self.assignments) + len(self.scores) + len(self.attendances) +
                 len(self.profiles) + len(self.advices))
        print(f'\nTotal records: {total:,}')

        self.write_sql(OUTPUT_FILE)
        print('\nDone!')


# ============================================================
# 入口
# ============================================================
if __name__ == '__main__':
    gen = SeedDataGenerator()
    gen.run()
