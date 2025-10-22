package com.knowledge.questioncard.config;

import com.knowledge.questioncard.entity.QuestionBank;
import com.knowledge.questioncard.entity.QuestionCard;
import com.knowledge.questioncard.mapper.QuestionBankMapper;
import com.knowledge.questioncard.mapper.QuestionCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final QuestionBankMapper questionBankMapper;
    private final QuestionCardMapper questionCardMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 检查并添加expire_time字段
        checkAndAddExpireTimeColumn();
        
        // 检查是否已有数据
        List<QuestionBank> existingBanks = questionBankMapper.selectAll();
        if (!existingBanks.isEmpty()) {
            // 如果已有数据,更新所有题库的统计信息
            System.out.println("检测到已有 " + existingBanks.size() + " 个题库,正在更新统计信息...");
            for (QuestionBank bank : existingBanks) {
                updateBankStatistics(bank.getId());
            }
            System.out.println("所有题库统计信息更新完成,DataInitializer执行结束");
            return;
        }

        // 初始化财务题库
        initFinanceBank();

        // 初始化税务题库
        initTaxBank();
        
        // 初始化法律题库
        initLegalBank();
        
        // 初始化管理题库
        initManagementBank();
        
        // 初始化市场营销题库
        initMarketingBank();
    }

    private void initFinanceBank() {
        QuestionBank bank = new QuestionBank();
        bank.setName("财务基础知识题库");
        bank.setDescription("涵盖财务会计基础知识的系统题库");
        bank.setTopic("财务");
        bank.setType("system");
        bank.setCreatedAt(new Date());
        questionBankMapper.insert(bank);

        List<QuestionCard> cards = new ArrayList<>();
        Long bankId = bank.getId();

        cards.add(createCard("什么是会计的基本假设?", 
            "会计的四个基本假设包括:1.会计主体假设 2.持续经营假设 3.会计分期假设 4.货币计量假设。这些假设是会计核算的前提条件。", bankId));
        
        cards.add(createCard("资产的定义是什么?", 
            "资产是指企业过去的交易或事项形成的、由企业拥有或控制的、预期会给企业带来经济利益的资源。资产具有三个特征:过去形成、现在拥有或控制、未来带来经济利益。", bankId));
        
        cards.add(createCard("什么是复式记账法?", 
            "复式记账法是对每一笔经济业务都要在两个或两个以上相互联系的账户中进行登记,系统地反映资金运动变化结果的记账方法。借贷记账法是目前最常用的复式记账法。", bankId));
        
        cards.add(createCard("现金流量表的作用是什么?", 
            "现金流量表反映企业在一定会计期间现金和现金等价物流入和流出的情况,分为经营活动、投资活动和筹资活动三类。它能够揭示企业的现金创造能力和支付能力。", bankId));
        
        cards.add(createCard("什么是应收账款周转率?", 
            "应收账款周转率是企业在一定时期内销售收入净额与平均应收账款余额的比率,反映应收账款的流动性。计算公式:应收账款周转率 = 销售收入净额 / 平均应收账款余额。", bankId));
        
        cards.add(createCard("长期股权投资的核算方法有哪些?", 
            "长期股权投资有两种核算方法:1.成本法-适用于对被投资单位不具有控制、共同控制或重大影响的情况 2.权益法-适用于对被投资单位具有共同控制或重大影响的情况。", bankId));
        
        cards.add(createCard("什么是坏账准备?", 
            "坏账准备是企业对可能发生坏账损失的应收账款计提的准备金。计提坏账准备遵循谨慎性原则,常用方法有账龄分析法、余额百分比法等。", bankId));
        
        cards.add(createCard("财务报表分析的主要方法有哪些?", 
            "财务报表分析的主要方法包括:1.比率分析法-计算各种财务比率 2.比较分析法-不同时期或企业间比较 3.趋势分析法-分析数据变化趋势 4.因素分析法-分析影响因素。", bankId));

        for (QuestionCard card : cards) {
            card.setCreatedAt(new Date());
            questionCardMapper.insert(card);
        }
        
        // 更新题库卡片数量统计
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
    }

    private void initTaxBank() {
        QuestionBank bank = new QuestionBank();
        bank.setName("税务基础知识题库");
        bank.setDescription("涵盖税收基础知识和实务操作的系统题库");
        bank.setTopic("税务");
        bank.setType("system");
        bank.setCreatedAt(new Date());
        questionBankMapper.insert(bank);

        List<QuestionCard> cards = new ArrayList<>();
        Long bankId = bank.getId();

        cards.add(createCard("我国现行的主要税种有哪些?", 
            "我国现行主要税种包括:增值税、企业所得税、个人所得税、消费税、城市维护建设税、教育费附加、房产税、土地增值税、印花税、车辆购置税等。其中增值税和企业所得税是最主要的两大税种。", bankId));
        
        cards.add(createCard("一般纳税人和小规模纳税人有什么区别?", 
            "主要区别:1.认定标准不同-年应税销售额500万元为界 2.税率不同-一般纳税人13%/9%/6%,小规模3% 3.计税方法不同-一般纳税人可抵扣进项,小规模不可抵扣 4.发票使用不同。", bankId));
        
        cards.add(createCard("什么是纳税筹划?", 
            "纳税筹划是指在法律规定许可的范围内,通过对经营、投资、理财活动的事先筹划和安排,尽可能取得节税的经济利益。纳税筹划必须合法合规,不同于偷税漏税。", bankId));
        
        cards.add(createCard("增值税专用发票和普通发票的区别?", 
            "主要区别:1.使用主体-专票限一般纳税人,普票无限制 2.抵扣功能-专票可抵扣进项税,普票不可 3.票面信息-专票信息更详细 4.联次-专票三联,普票两联。", bankId));
        
        cards.add(createCard("什么是税收优惠政策?", 
            "税收优惠政策是国家为了实现特定经济社会目标,对某些纳税人和征税对象给予鼓励和照顾的特殊规定。常见形式:免税、减税、退税、税收抵免、优惠税率等。", bankId));
        
        cards.add(createCard("企业所得税的税前扣除原则是什么?", 
            "税前扣除应遵循的原则:1.相关性原则-与取得收入相关 2.合理性原则-符合常规 3.确定性原则-实际发生 4.合法性原则-符合法规。不符合这些原则的支出不得税前扣除。", bankId));
        
        cards.add(createCard("什么是视同销售?", 
            "视同销售是指企业虽然没有实际销售货物或提供服务,但税法上将其视为销售行为需要缴纳增值税。如:将货物用于非增值税应税项目、无偿赠送他人等八种情形。", bankId));
        
        cards.add(createCard("个人所得税的专项附加扣除有哪些?", 
            "个人所得税专项附加扣除包括七项:1.子女教育 2.继续教育 3.大病医疗 4.住房贷款利息 5.住房租金 6.赡养老人 7.3岁以下婴幼儿照护。纳税人可按规定标准扣除。", bankId));

        for (QuestionCard card : cards) {
            card.setCreatedAt(new Date());
            questionCardMapper.insert(card);
        }
        
        // 更新题库卡片数量统计
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
    }

    private void initLegalBank() {
        QuestionBank bank = new QuestionBank();
        bank.setName("法律基础知识题库");
        bank.setDescription("涵盖企业常用法律知识的系统题库");
        bank.setTopic("法律");
        bank.setType("system");
        bank.setCreatedAt(new Date());
        questionBankMapper.insert(bank);

        List<QuestionCard> cards = new ArrayList<>();
        Long bankId = bank.getId();

        cards.add(createCard("什么是合同?", 
            "合同是平等主体的自然人、法人、其他组织之间设立、变更、终止民事权利义务关系的协议。合同的订立应遵循平等、自愿、公平、诚实信用原则。", bankId));
        
        cards.add(createCard("合同的有效要件有哪些?", 
            "合同有效要件包括:1.当事人具有相应民事行为能力 2.意思表示真实 3.不违反法律、行政法规的强制性规定 4.不违背公序良俗。满足这些条件的合同依法成立并生效。", bankId));
        
        cards.add(createCard("什么是违约责任?", 
            "违约责任是指当事人不履行合同义务或履行合同义务不符合约定时,依法应承担的民事责任。违约责任形式包括:继续履行、采取补救措施、赔偿损失、支付违约金等。", bankId));
        
        cards.add(createCard("劳动合同应包含哪些必备条款?", 
            "劳动合同必备条款包括:1.用人单位和劳动者信息 2.合同期限 3.工作内容和地点 4.工作时间和休息休假 5.劳动报酬 6.社会保险 7.劳动保护和条件 8.其他事项。", bankId));
        
        cards.add(createCard("什么是知识产权?", 
            "知识产权是指人们对智力劳动成果依法享有的专有权利。主要包括:著作权、专利权、商标权、商业秘密等。知识产权具有专有性、地域性和时间性特征。", bankId));
        
        cards.add(createCard("公司的法定代表人有什么职责?", 
            "法定代表人是依法代表法人行使民事权利、履行民事义务的负责人。职责包括:对外代表公司、签署重要文件、主持公司重大经营决策、承担相应法律责任等。", bankId));
        
        cards.add(createCard("什么是诉讼时效?", 
            "诉讼时效是指权利人在法定期间内不行使权利,该期间届满后,义务人获得抗辩权的制度。普通诉讼时效期间为三年,自权利人知道或应当知道权利受到损害之日起计算。", bankId));
        
        cards.add(createCard("企业常见的法律风险有哪些?", 
            "企业常见法律风险包括:1.合同风险-合同条款不完善、履行纠纷 2.劳动用工风险-劳动合同管理不规范 3.知识产权风险-侵权或被侵权 4.税务风险 5.环保合规风险等。", bankId));

        for (QuestionCard card : cards) {
            card.setCreatedAt(new Date());
            questionCardMapper.insert(card);
        }
        
        // 更新题库卡片数量统计
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
    }

    private void initManagementBank() {
        QuestionBank bank = new QuestionBank();
        bank.setName("企业管理知识题库");
        bank.setDescription("涵盖企业管理理论与实践的系统题库");
        bank.setTopic("管理");
        bank.setType("system");
        bank.setCreatedAt(new Date());
        questionBankMapper.insert(bank);

        List<QuestionCard> cards = new ArrayList<>();
        Long bankId = bank.getId();

        cards.add(createCard("什么是PDCA循环?", 
            "PDCA循环是管理学中的一个通用模型,包括计划(Plan)、执行(Do)、检查(Check)、处理(Act)四个阶段。它是质量管理的基本方法,强调持续改进。", bankId));
        
        cards.add(createCard("管理的五大职能是什么?", 
            "管理的五大职能包括:1.计划-确定目标和实现路径 2.组织-配置资源和建立结构 3.领导-激励和指导员工 4.控制-监督和纠偏 5.协调-整合各方资源。这是法约尔提出的经典理论。", bankId));
        
        cards.add(createCard("什么是KPI?", 
            "KPI(Key Performance Indicator)即关键绩效指标,是衡量组织战略目标实现程度的量化指标。KPI应具备SMART特征:具体的、可衡量的、可实现的、相关的、有时限的。", bankId));
        
        cards.add(createCard("马斯洛需求层次理论包括哪些层次?", 
            "马斯洛需求层次理论将人的需求分为五个层次:1.生理需求-基本生存需要 2.安全需求-稳定和保护 3.社交需求-归属和爱 4.尊重需求-认可和地位 5.自我实现需求-发挥潜能。", bankId));
        
        cards.add(createCard("什么是SWOT分析?", 
            "SWOT分析是战略规划的常用工具,分析企业的优势(Strengths)、劣势(Weaknesses)、机会(Opportunities)和威胁(Threats),帮助企业制定发展战略。", bankId));
        
        cards.add(createCard("项目管理的铁三角是什么?", 
            "项目管理铁三角指项目管理的三大约束条件:范围(Scope)、时间(Time)、成本(Cost)。这三者相互制约,改变其中一个必然影响其他两个,需要平衡管理。", bankId));
        
        cards.add(createCard("什么是扁平化管理?", 
            "扁平化管理是指减少管理层级,压缩组织结构,使组织更加灵活高效。其优点是决策快、沟通顺畅、反应灵活;挑战是管理幅度大、需要员工自主性强。", bankId));
        
        cards.add(createCard("企业文化的核心要素有哪些?", 
            "企业文化核心要素包括:1.价值观-企业的核心理念 2.使命-企业存在的意义 3.愿景-企业未来的目标 4.行为规范-员工的行为准则 5.物质载体-可见的文化表现形式。", bankId));

        for (QuestionCard card : cards) {
            card.setCreatedAt(new Date());
            questionCardMapper.insert(card);
        }
        
        // 更新题库卡片数量统计
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
    }

    private void initMarketingBank() {
        QuestionBank bank = new QuestionBank();
        bank.setName("市场营销知识题库");
        bank.setDescription("涵盖市场营销理论与实践的系统题库");
        bank.setTopic("市场营销");
        bank.setType("system");
        bank.setCreatedAt(new Date());
        questionBankMapper.insert(bank);

        List<QuestionCard> cards = new ArrayList<>();
        Long bankId = bank.getId();

        cards.add(createCard("什么是4P营销理论?", 
            "4P营销理论是由麦卡锡提出的经典营销组合,包括:产品(Product)、价格(Price)、渠道(Place)、促销(Promotion)。企业通过优化这四个要素来满足目标市场需求。", bankId));
        
        cards.add(createCard("市场细分的依据有哪些?", 
            "市场细分的主要依据包括:1.地理因素-国家、地区、城市 2.人口因素-年龄、性别、收入、教育 3.心理因素-生活方式、个性、价值观 4.行为因素-购买动机、使用频率、品牌忠诚度。", bankId));
        
        cards.add(createCard("什么是品牌定位?", 
            "品牌定位是指企业在目标消费者心智中建立与众不同的品牌形象和价值主张。好的品牌定位应该明确、独特、有价值、可信,并能持续传播。", bankId));
        
        cards.add(createCard("产品生命周期包括哪些阶段?", 
            "产品生命周期包括四个阶段:1.导入期-产品刚推出,销量低 2.成长期-销量快速增长 3.成熟期-销量达到峰值后趋于稳定 4.衰退期-销量下降。不同阶段需要不同的营销策略。", bankId));
        
        cards.add(createCard("什么是内容营销?", 
            "内容营销是通过创造和分发有价值、相关且一致的内容,吸引并留住明确定义的目标受众,最终驱动有利可图的客户行动。重点在于提供价值而非直接推销。", bankId));
        
        cards.add(createCard("消费者购买决策过程包括哪些步骤?", 
            "消费者购买决策过程包括五个步骤:1.认识需求-意识到问题 2.信息搜集-收集产品信息 3.评估比较-比较各种选择 4.购买决策-做出购买 5.购后评价-使用后的满意度评估。", bankId));
        
        cards.add(createCard("什么是客户关系管理(CRM)?", 
            "CRM是企业为提高核心竞争力,利用信息技术手段对客户进行系统化管理的方法。目标是建立、维护和发展与客户的长期关系,提高客户满意度和忠诚度。", bankId));
        
        cards.add(createCard("社交媒体营销的优势是什么?", 
            "社交媒体营销的优势包括:1.成本低-相比传统媒体更经济 2.互动性强-可与客户实时沟通 3.传播快-病毒式传播效应 4.精准定位-可针对特定群体 5.数据可追踪-效果可量化分析。", bankId));

        for (QuestionCard card : cards) {
            card.setCreatedAt(new Date());
            questionCardMapper.insert(card);
        }
        
        // 更新题库卡片数量统计
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
    }

    private void updateBankStatistics(Long bankId) {
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
        System.out.println("题库 ID=" + bankId + " 的卡片数量已更新为: " + cardCount);
    }

    private void checkAndAddExpireTimeColumn() {
        try {
            // 检查expire_time字段是否存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'question_banks' " +
                    "AND COLUMN_NAME = 'expire_time'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (count != null && count == 0) {
                // 字段不存在,添加字段
                String addColumnSql = "ALTER TABLE question_banks ADD COLUMN expire_time DATETIME DEFAULT NULL COMMENT '分享过期时间'";
                jdbcTemplate.execute(addColumnSql);
                System.out.println("成功添加expire_time字段到question_banks表");
            } else {
                System.out.println("expire_time字段已存在,跳过添加");
            }
        } catch (Exception e) {
            System.err.println("检查或添加expire_time字段时出错: " + e.getMessage());
        }
    }

    private QuestionCard createCard(String question, String answer, Long bankId) {
        QuestionCard card = new QuestionCard();
        card.setQuestion(question);
        card.setAnswer(answer);
        card.setBankId(bankId);
        return card;
    }
}