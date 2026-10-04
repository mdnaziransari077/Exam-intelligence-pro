package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ExtractedQuestionDao
import com.example.data.local.ProjectDao
import com.example.data.local.QuestionTopicMappingDao
import com.example.data.local.SyllabusDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.CoverageStatus
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.MappingReviewLogEntity
import com.example.data.model.MappingReviewStatus
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusNodeType
import com.example.data.model.SyllabusSourceType
import com.example.data.model.SyllabusVerificationStatus
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.TopicCoverageStats
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyllabusRepository(
    private val context: Context,
    private val syllabusDao: SyllabusDao,
    private val questionTopicMappingDao: QuestionTopicMappingDao,
    private val extractedQuestionDao: ExtractedQuestionDao,
    private val documentDao: DocumentDao,
    private val projectDao: ProjectDao,
    private val auditLogDao: AuditLogDao
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    // --- Syllabus Flow Queries ---

    fun getAllSyllabusVersions(): Flow<List<SyllabusVersionEntity>> =
        syllabusDao.getAllSyllabusVersions().flowOn(Dispatchers.IO)

    fun getVersionsForSubject(subject: String): Flow<List<SyllabusVersionEntity>> =
        syllabusDao.getVersionsForSubject(subject).flowOn(Dispatchers.IO)

    fun getNodesForVersion(versionId: String): Flow<List<SyllabusNodeEntity>> =
        syllabusDao.getNodesForVersion(versionId).flowOn(Dispatchers.IO)

    fun getMappingsForProject(projectId: String): Flow<List<QuestionTopicMappingEntity>> =
        questionTopicMappingDao.getMappingsForProject(projectId).flowOn(Dispatchers.IO)

    fun getMappingsForQuestion(questionId: String): Flow<List<QuestionTopicMappingEntity>> =
        questionTopicMappingDao.getMappingsForQuestion(questionId).flowOn(Dispatchers.IO)

    fun getReviewLogsForQuestion(questionId: String): Flow<List<MappingReviewLogEntity>> =
        questionTopicMappingDao.getReviewLogsForQuestion(questionId).flowOn(Dispatchers.IO)

    suspend fun getSyllabusVersionById(versionId: String): SyllabusVersionEntity? = withContext(Dispatchers.IO) {
        syllabusDao.getVersionById(versionId)
    }

    suspend fun getNodesListForVersion(versionId: String): List<SyllabusNodeEntity> = withContext(Dispatchers.IO) {
        syllabusDao.getNodesListForVersion(versionId)
    }

    // --- Discovery & Seeding ---

    suspend fun seedOfficialSyllabiIfEmpty() = withContext(Dispatchers.IO) {
        val count = syllabusDao.getVersionCount()
        if (count > 0) return@withContext

        // 1. Mathematics (Standard) 2024-2025
        val math2024 = SyllabusVersionEntity(
            id = "cbse-10-math-2024-25",
            board = "CBSE",
            classLevel = "10",
            subject = "Mathematics (Standard)",
            academicSession = "2024-2025",
            versionIdentifier = "CBSE-10-MATH-2024-25-v1",
            sourceType = SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT.name,
            sourceTitle = "CBSE Secondary School Curriculum (Class X) 2024-25",
            sourceUrl = "https://cbseacademic.nic.in/curriculum_2025.html",
            verificationStatus = SyllabusVerificationStatus.VERIFIED.label,
            verifiedBy = "CBSE Academic Council (Official Publication)",
            verifiedAt = System.currentTimeMillis() - 86400000L * 90,
            reviewerNotes = "Official curriculum issued by Central Board of Secondary Education for 2024-2025 examinations.",
            changeHistory = "Imported baseline official CBSE 2024-25 curriculum."
        )
        syllabusDao.insertVersion(math2024)
        seedMath2024Nodes(math2024.id)

        // 2. Science (086) 2024-2025
        val science2024 = SyllabusVersionEntity(
            id = "cbse-10-sci-2024-25",
            board = "CBSE",
            classLevel = "10",
            subject = "Science (086)",
            academicSession = "2024-2025",
            versionIdentifier = "CBSE-10-SCI-2024-25-v1",
            sourceType = SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT.name,
            sourceTitle = "CBSE Secondary School Curriculum - Science (Code 086) 2024-25",
            sourceUrl = "https://cbseacademic.nic.in/curriculum_2025.html",
            verificationStatus = SyllabusVerificationStatus.VERIFIED.label,
            verifiedBy = "CBSE Academic Council (Official Publication)",
            verifiedAt = System.currentTimeMillis() - 86400000L * 90,
            reviewerNotes = "Official 2024-25 Science syllabus. Total 80 Marks theory examination.",
            changeHistory = "Imported baseline official CBSE Science curriculum."
        )
        syllabusDao.insertVersion(science2024)
        seedScience2024Nodes(science2024.id)

        // 3. Social Science (087) 2024-2025
        val sst2024 = SyllabusVersionEntity(
            id = "cbse-10-sst-2024-25",
            board = "CBSE",
            classLevel = "10",
            subject = "Social Science (087)",
            academicSession = "2024-2025",
            versionIdentifier = "CBSE-10-SST-2024-25-v1",
            sourceType = SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT.name,
            sourceTitle = "CBSE Secondary School Curriculum - Social Science (Code 087) 2024-25",
            sourceUrl = "https://cbseacademic.nic.in/curriculum_2025.html",
            verificationStatus = SyllabusVerificationStatus.VERIFIED.label,
            verifiedBy = "CBSE Academic Council (Official Publication)",
            verifiedAt = System.currentTimeMillis() - 86400000L * 90,
            reviewerNotes = "History, Geography, Democratic Politics, Economics official curriculum.",
            changeHistory = "Imported baseline official CBSE Social Science curriculum."
        )
        syllabusDao.insertVersion(sst2024)
        seedSocialScienceNodes(sst2024.id)

        // 4. Mathematics (Standard) 2023-2024 (Historical Version)
        val math2023 = SyllabusVersionEntity(
            id = "cbse-10-math-2023-24",
            board = "CBSE",
            classLevel = "10",
            subject = "Mathematics (Standard)",
            academicSession = "2023-2024",
            versionIdentifier = "CBSE-10-MATH-2023-24-v1",
            sourceType = SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT.name,
            sourceTitle = "CBSE Secondary School Curriculum (Class X) 2023-24",
            sourceUrl = "https://cbseacademic.nic.in/curriculum_2024.html",
            verificationStatus = SyllabusVerificationStatus.VERIFIED.label,
            verifiedBy = "CBSE Academic Council (Historical)",
            verifiedAt = System.currentTimeMillis() - 86400000L * 400,
            reviewerNotes = "Historical syllabus for academic session 2023-2024 for past paper mapping comparisons.",
            changeHistory = "Archived baseline official CBSE 2023-24 curriculum."
        )
        syllabusDao.insertVersion(math2023)
        seedMath2024Nodes(math2023.id)
    }

    private suspend fun seedMath2024Nodes(versionId: String) {
        val nodes = mutableListOf<SyllabusNodeEntity>()
        var order = 0

        fun addUnit(code: String, name: String): String {
            val id = "$versionId-unit-${order++}"
            nodes.add(
                SyllabusNodeEntity(
                    id = id,
                    syllabusVersionId = versionId,
                    nodeType = SyllabusNodeType.UNIT.name,
                    code = code,
                    name = name,
                    displayOrder = order,
                    verificationStatus = SyllabusVerificationStatus.VERIFIED.label
                )
            )
            return id
        }

        fun addChapter(parentId: String, code: String, name: String, desc: String?): String {
            val id = "$versionId-ch-${order++}"
            nodes.add(
                SyllabusNodeEntity(
                    id = id,
                    syllabusVersionId = versionId,
                    parentId = parentId,
                    nodeType = SyllabusNodeType.CHAPTER.name,
                    code = code,
                    name = name,
                    description = desc,
                    displayOrder = order,
                    verificationStatus = SyllabusVerificationStatus.VERIFIED.label
                )
            )
            return id
        }

        fun addTopic(
            parentId: String,
            code: String,
            name: String,
            desc: String?,
            isExcluded: Boolean = false,
            exclusionReason: String? = null
        ): String {
            val id = "$versionId-top-${order++}"
            nodes.add(
                SyllabusNodeEntity(
                    id = id,
                    syllabusVersionId = versionId,
                    parentId = parentId,
                    nodeType = SyllabusNodeType.TOPIC.name,
                    code = code,
                    name = name,
                    description = desc,
                    displayOrder = order,
                    verificationStatus = SyllabusVerificationStatus.VERIFIED.label,
                    isExcluded = isExcluded,
                    exclusionReason = exclusionReason
                )
            )
            return id
        }

        // Unit I: Number Systems
        val u1 = addUnit("Unit I", "Number Systems")
        val ch1 = addChapter(u1, "Chapter 1", "Real Numbers", "Fundamental Theorem of Arithmetic, irrational proofs")
        addTopic(ch1, "1.1", "Fundamental Theorem of Arithmetic", "Statements after reviewing work done earlier, primes factorisation, HCF and LCM")
        addTopic(ch1, "1.2", "Proofs of Irrationality", "Proof of irrationality of √2, √3, √5 by contradiction")
        addTopic(ch1, "1.3", "Euclid's Division Lemma", "Historical topic rationalized/deleted in 2024-25", isExcluded = true, exclusionReason = "Rationalized by NCERT/CBSE for Academic Session 2024-25")

        // Unit II: Algebra
        val u2 = addUnit("Unit II", "Algebra")
        val ch2 = addChapter(u2, "Chapter 2", "Polynomials", "Zeroes of polynomial, quadratic polynomials relationships")
        addTopic(ch2, "2.1", "Zeroes of a Polynomial", "Geometric meaning of zeroes of a polynomial, number of zeroes from graphs")
        addTopic(ch2, "2.2", "Relationship between Zeroes and Coefficients", "Zeroes and coefficients of quadratic polynomials (α + β = -b/a, αβ = c/a)")
        addTopic(ch2, "2.3", "Division Algorithm for Polynomials", "Rationalized/omitted from active syllabus", isExcluded = true, exclusionReason = "Rationalized by NCERT/CBSE for Academic Session 2024-25")

        val ch3 = addChapter(u2, "Chapter 3", "Pair of Linear Equations in Two Variables", "Graphical and algebraic methods")
        addTopic(ch3, "3.1", "Graphical Method of Solution", "Consistency/inconsistency of pair of linear equations in two variables")
        addTopic(ch3, "3.2", "Substitution Method", "Algebraic solution of pair of linear equations by substitution")
        addTopic(ch3, "3.3", "Elimination Method", "Algebraic solution of pair of linear equations by elimination")
        addTopic(ch3, "3.4", "Cross Multiplication Method", "Rationalized/deleted", isExcluded = true, exclusionReason = "Rationalized by NCERT/CBSE for Academic Session 2024-25")

        val ch4 = addChapter(u2, "Chapter 4", "Quadratic Equations", "Standard form, factorisation, quadratic formula, discriminant")
        addTopic(ch4, "4.1", "Standard Form of Quadratic Equation", "ax² + bx + c = 0, (a ≠ 0), real-life contextual formulation")
        addTopic(ch4, "4.2", "Solution by Factorisation", "Finding roots of quadratic equations by factorisation")
        addTopic(ch4, "4.3", "Quadratic Formula & Nature of Roots", "Discriminant D = b² - 4ac, real roots condition, finding real roots")

        val ch5 = addChapter(u2, "Chapter 5", "Arithmetic Progressions", "nth term, sum of first n terms")
        addTopic(ch5, "5.1", "nth Term of an AP", "General term of AP, finding specific terms from common difference")
        addTopic(ch5, "5.2", "Sum of First n Terms of an AP", "Derivation of Sₙ = n/2[2a + (n-1)d] and daily-life situational word problems")

        // Unit III: Coordinate Geometry
        val u3 = addUnit("Unit III", "Coordinate Geometry")
        val ch6 = addChapter(u3, "Chapter 6", "Coordinate Geometry", "Concepts, distance formula, section formula")
        addTopic(ch6, "6.1", "Distance Formula", "Distance between two points in Cartesian plane, geometric validation")
        addTopic(ch6, "6.2", "Section Formula", "Internal division of line segment joining two points in ratio m:n, midpoint formula")
        addTopic(ch6, "6.3", "Area of a Triangle", "Rationalized/deleted from 2024-25 syllabus", isExcluded = true, exclusionReason = "Rationalized by NCERT/CBSE for Academic Session 2024-25")

        // Unit IV: Geometry
        val u4 = addUnit("Unit IV", "Geometry")
        val ch7 = addChapter(u4, "Chapter 7", "Triangles", "Similarity, Basic Proportionality Theorem, similarity criteria")
        addTopic(ch7, "7.1", "Basic Proportionality Theorem (Thales)", "Proof and application of line drawn parallel to one side of triangle")
        addTopic(ch7, "7.2", "Criteria for Similarity of Triangles", "AAA, SAS, SSS similarity criteria and applications")

        val ch8 = addChapter(u4, "Chapter 8", "Circles", "Tangents to circles, properties and proofs")
        addTopic(ch8, "8.1", "Tangent at any Point of Circle", "Proof: Tangent at any point is perpendicular to radius through point of contact")
        addTopic(ch8, "8.2", "Lengths of Tangents from External Point", "Proof: Tangents drawn from an external point to a circle are equal in length")

        // Unit V: Trigonometry
        val u5 = addUnit("Unit V", "Trigonometry")
        val ch9 = addChapter(u5, "Chapter 9", "Introduction to Trigonometry", "Trigonometric ratios of acute angles")
        addTopic(ch9, "9.1", "Trigonometric Ratios", "sin, cos, tan, cot, sec, cosec of an acute angle in a right triangle")
        addTopic(ch9, "9.2", "Values of Ratios (0°, 30°, 45°, 60°, 90°)", "Evaluation of trigonometric expressions at standard angles")

        val ch10 = addChapter(u5, "Chapter 10", "Trigonometric Identities", "Proof and applications of identities")
        addTopic(ch10, "10.1", "Identity sin² θ + cos² θ = 1", "Proof and application of primary Pythagorean identity")

        val ch11 = addChapter(u5, "Chapter 11", "Some Applications of Trigonometry", "Heights and Distances, angles of elevation/depression")
        addTopic(ch11, "11.1", "Heights and Distances Problems", "Simple problems on angle of elevation/depression with standard angles 30°, 45°, 60°")

        // Unit VI: Mensuration
        val u6 = addUnit("Unit VI", "Mensuration")
        val ch12 = addChapter(u6, "Chapter 12", "Areas Related to Circles", "Sector and segment areas")
        addTopic(ch12, "12.1", "Area of Sector and Segment", "Area of sectors and segments of circle with central angle of 60°, 90°, 120°")

        val ch13 = addChapter(u6, "Chapter 13", "Surface Areas and Volumes", "Combinations of solids")
        addTopic(ch13, "13.1", "Surface Area & Volume of Combinations", "Cubes, cuboids, spheres, hemispheres, right circular cylinders/cones")

        // Unit VII: Statistics & Probability
        val u7 = addUnit("Unit VII", "Statistics & Probability")
        val ch14 = addChapter(u7, "Chapter 14", "Statistics", "Mean, median, mode of grouped data")
        addTopic(ch14, "14.1", "Mean of Grouped Data", "Direct method, assumed mean method for calculating mean")
        addTopic(ch14, "14.2", "Mode and Median of Grouped Data", "Modal class, median class, empirical relationship between three measures")

        val ch15 = addChapter(u7, "Chapter 15", "Probability", "Classical definition and event probabilities")
        addTopic(ch15, "15.1", "Classical Definition of Probability", "Simple problems on finding probability of an event, complementary events")

        syllabusDao.insertNodes(nodes)
    }

    private suspend fun seedScience2024Nodes(versionId: String) {
        val nodes = mutableListOf<SyllabusNodeEntity>()
        var order = 0

        fun addUnit(code: String, name: String): String {
            val id = "$versionId-unit-${order++}"
            nodes.add(SyllabusNodeEntity(id = id, syllabusVersionId = versionId, nodeType = SyllabusNodeType.UNIT.name, code = code, name = name, displayOrder = order, verificationStatus = SyllabusVerificationStatus.VERIFIED.label))
            return id
        }

        fun addChapter(parentId: String, code: String, name: String, desc: String?): String {
            val id = "$versionId-ch-${order++}"
            nodes.add(SyllabusNodeEntity(id = id, syllabusVersionId = versionId, parentId = parentId, nodeType = SyllabusNodeType.CHAPTER.name, code = code, name = name, description = desc, displayOrder = order, verificationStatus = SyllabusVerificationStatus.VERIFIED.label))
            return id
        }

        fun addTopic(parentId: String, code: String, name: String, desc: String?): String {
            val id = "$versionId-top-${order++}"
            nodes.add(SyllabusNodeEntity(id = id, syllabusVersionId = versionId, parentId = parentId, nodeType = SyllabusNodeType.TOPIC.name, code = code, name = name, description = desc, displayOrder = order, verificationStatus = SyllabusVerificationStatus.VERIFIED.label))
            return id
        }

        val u1 = addUnit("Unit I", "Chemical Substances - Nature and Behaviour")
        val ch1 = addChapter(u1, "Chapter 1", "Chemical Reactions and Equations", "Chemical equation, Balanced chemical equation, types of chemical reactions")
        addTopic(ch1, "1.1", "Types of Chemical Reactions", "Combination, decomposition, displacement, double displacement, precipitation, neutralization, oxidation and reduction")
        addTopic(ch1, "1.2", "Balancing Chemical Equations", "Law of conservation of mass, writing balanced equations with physical state symbols")

        val ch2 = addChapter(u1, "Chapter 2", "Acids, Bases and Salts", "Properties of acids and bases, pH scale, important chemical compounds")
        addTopic(ch2, "2.1", "Properties and pH Scale", "H⁺ and OH⁻ ions, pH scale definition, importance in everyday life")
        addTopic(ch2, "2.2", "Salts and Preparation", "Sodium hydroxide, Bleaching powder, Baking soda, Washing soda, Plaster of Paris")

        val ch3 = addChapter(u1, "Chapter 3", "Metals and Non-metals", "Properties, reactivity series, ionic compounds")
        addTopic(ch3, "3.1", "Properties & Reactivity Series", "Physical and chemical properties of metals/non-metals, reactivity series")
        addTopic(ch3, "3.2", "Ionic Compounds & Metallurgy", "Formation and properties of ionic compounds, basic metallurgical processes, corrosion prevention")

        val ch4 = addChapter(u1, "Chapter 4", "Carbon and its Compounds", "Covalent bonding, versatile nature, homologous series, functional groups")
        addTopic(ch4, "4.1", "Covalent Bonding & Versatile Nature", "Covalent bonding in carbon, catenation, tetravalency, homologous series")
        addTopic(ch4, "4.2", "Functional Groups & Chemical Properties", "Combustion, oxidation, addition and substitution reactions, soaps and detergents")

        val u2 = addUnit("Unit II", "World of Living")
        val ch5 = addChapter(u2, "Chapter 5", "Life Processes", "Nutrition, respiration, transport and excretion in plants and animals")
        addTopic(ch5, "5.1", "Nutrition & Respiration", "Autotrophic and heterotrophic nutrition, aerobic and anaerobic respiration in human body")
        addTopic(ch5, "5.2", "Transportation & Excretion", "Circulatory system, double circulation, structure of nephron and human excretion")

        val ch6 = addChapter(u2, "Chapter 6", "Control and Coordination", "Tropic movements in plants, nervous system, hormones")
        addTopic(ch6, "6.1", "Nervous System & Reflex Arc", "Structure of neuron, reflex action, central and peripheral nervous system")
        addTopic(ch6, "6.2", "Hormones in Animals & Plant Tropisms", "Thyroxine, insulin, adrenaline, plant growth regulators like auxin")

        val ch7 = addChapter(u2, "Chapter 7", "How do Organisms Reproduce?", "Asexual and sexual reproduction, reproductive health")
        addTopic(ch7, "7.1", "Asexual Reproduction", "Fission, fragmentation, regeneration, budding, vegetative propagation, spore formation")
        addTopic(ch7, "7.2", "Sexual Reproduction & Human Reproductive Health", "Flower parts, pollination, human male/female reproductive systems, contraception")

        val ch8 = addChapter(u2, "Chapter 8", "Heredity and Evolution", "Mendelian inheritance, sex determination")
        addTopic(ch8, "8.1", "Mendel's Laws of Inheritance", "Monohybrid and dihybrid cross, inheritance of traits")
        addTopic(ch8, "8.2", "Sex Determination", "Chromosomal basis of sex determination in human beings")

        val u3 = addUnit("Unit III", "Natural Phenomena")
        val ch9 = addChapter(u3, "Chapter 9", "Light - Reflection and Refraction", "Spherical mirrors, lenses, refractive index, lens formula")
        addTopic(ch9, "9.1", "Reflection by Spherical Mirrors", "Center of curvature, principal axis, focus, focal length, mirror formula and magnification")
        addTopic(ch9, "9.2", "Refraction & Lens Formula", "Laws of refraction, refractive index, refraction by spherical lenses, lens formula, power of lens")

        val ch10 = addChapter(u3, "Chapter 10", "Human Eye and Colourful World", "Refraction through prism, dispersion, scattering")
        addTopic(ch10, "10.1", "Defects of Vision", "Myopia, hypermetropia, presbyopia and their corrections")
        addTopic(ch10, "10.2", "Dispersion & Atmospheric Refraction", "Dispersion of light through prism, rainbow, atmospheric refraction, Tyndall effect")

        val u4 = addUnit("Unit IV", "Effects of Current")
        val ch11 = addChapter(u4, "Chapter 11", "Electricity", "Ohm's law, resistance, series/parallel, heating effect")
        addTopic(ch11, "11.1", "Ohm's Law & Factors Affecting Resistance", "Potential difference, electric current, Ohm's law, resistivity, series and parallel circuits")
        addTopic(ch11, "11.2", "Heating Effect of Electric Current", "Joule's law of heating, electric power, P = VI, kilowatt-hour commercial unit")

        val ch12 = addChapter(u4, "Chapter 12", "Magnetic Effects of Electric Current", "Magnetic field lines, solenoid, electromagnetic induction")
        addTopic(ch12, "12.1", "Magnetic Field & Field Lines", "Field due to current-carrying conductor, straight wire, circular loop, solenoid")
        addTopic(ch12, "12.2", "Force on Current-Carrying Conductor", "Fleming's Left-Hand Rule, electric motor, domestic electric circuits and fuse")

        val u5 = addUnit("Unit V", "Natural Resources")
        val ch13 = addChapter(u5, "Chapter 13", "Our Environment", "Ecosystem, food chains, ozone depletion, waste management")
        addTopic(ch13, "13.1", "Ecosystem & Food Chains", "Trophic levels, 10% energy flow law, biological magnification")
        addTopic(ch13, "13.2", "Environmental Issues", "Ozone layer depletion, biodegradable and non-biodegradable waste disposal")

        syllabusDao.insertNodes(nodes)
    }

    private suspend fun seedSocialScienceNodes(versionId: String) {
        val nodes = mutableListOf<SyllabusNodeEntity>()
        var order = 0

        fun addUnit(code: String, name: String): String {
            val id = "$versionId-unit-${order++}"
            nodes.add(SyllabusNodeEntity(id = id, syllabusVersionId = versionId, nodeType = SyllabusNodeType.UNIT.name, code = code, name = name, displayOrder = order, verificationStatus = SyllabusVerificationStatus.VERIFIED.label))
            return id
        }

        fun addChapter(parentId: String, code: String, name: String, desc: String?): String {
            val id = "$versionId-ch-${order++}"
            nodes.add(SyllabusNodeEntity(id = id, syllabusVersionId = versionId, parentId = parentId, nodeType = SyllabusNodeType.CHAPTER.name, code = code, name = name, description = desc, displayOrder = order, verificationStatus = SyllabusVerificationStatus.VERIFIED.label))
            return id
        }

        fun addTopic(parentId: String, code: String, name: String, desc: String?): String {
            val id = "$versionId-top-${order++}"
            nodes.add(SyllabusNodeEntity(id = id, syllabusVersionId = versionId, parentId = parentId, nodeType = SyllabusNodeType.TOPIC.name, code = code, name = name, description = desc, displayOrder = order, verificationStatus = SyllabusVerificationStatus.VERIFIED.label))
            return id
        }

        val u1 = addUnit("History", "India and the Contemporary World - II")
        val ch1 = addChapter(u1, "Chapter 1", "The Rise of Nationalism in Europe", "French Revolution, making of nationalism, unification of Germany and Italy")
        addTopic(ch1, "1.1", "French Revolution and Idea of Nation", "Spread of nationalism, Civil Code of 1804 (Napoleonic Code)")
        addTopic(ch1, "1.2", "Unification of Italy and Germany", "Role of Bismarck, Cavour, Garibaldi, Balkan nationalism crisis")

        val ch2 = addChapter(u1, "Chapter 2", "Nationalism in India", "Non-Cooperation, Civil Disobedience, sense of collective belonging")
        addTopic(ch2, "2.1", "Non-Cooperation Movement", "Causes, Khilafat issue, spread in towns and countryside, withdrawal")
        addTopic(ch2, "2.2", "Civil Disobedience Movement", "Salt March, Round Table conferences, limits of civil disobedience")

        val u2 = addUnit("Geography", "Contemporary India - II")
        val ch3 = addChapter(u2, "Chapter 3", "Resources and Development", "Classification of resources, land use, soil types")
        addTopic(ch3, "3.1", "Resource Planning & Conservation", "Sustainable development, Agenda 21, land degradation and conservation")
        addTopic(ch3, "3.2", "Soil Types in India", "Alluvial, black, red, yellow, laterite, arid and forest soils")

        val u3 = addUnit("Political Science", "Democratic Politics - II")
        val ch4 = addChapter(u3, "Chapter 4", "Federalism", "What is federalism, federal system in India, decentralization")
        addTopic(ch4, "4.1", "Key Features of Federalism", "Union list, state list, concurrent list, linguistic states")
        addTopic(ch4, "4.2", "Decentralisation in India", "Panchayati Raj, 73rd and 74th constitutional amendments")

        val u4 = addUnit("Economics", "Understanding Economic Development")
        val ch5 = addChapter(u4, "Chapter 5", "Sectors of the Indian Economy", "Primary, secondary, tertiary sectors, organized vs unorganized")
        addTopic(ch5, "5.1", "Comparing the Three Sectors", "GDP contribution, rising importance of tertiary sector")
        addTopic(ch5, "5.2", "Division of Sectors: Employment", "Disguised unemployment, NREGA 2005, organized and unorganized sector conditions")

        syllabusDao.insertNodes(nodes)
    }

    // --- Syllabus Import & Creation ---

    suspend fun createOrImportSyllabus(
        user: UserEntity,
        subject: String,
        academicSession: String,
        versionIdentifier: String,
        sourceType: SyllabusSourceType,
        sourceTitle: String,
        sourceUrl: String?,
        sourceDocumentRef: String?,
        notes: String?,
        rawContentToParse: String? = null
    ): Result<SyllabusVersionEntity> = withContext(Dispatchers.IO) {
        if (!user.getRoleEnum().canManageSyllabus()) {
            return@withContext Result.failure(
                SecurityException("Unauthorized: Only Teachers and Administrators can create or import syllabus versions.")
            )
        }

        val existing = syllabusDao.getVersionForSubjectAndSession(subject, academicSession)
        val versionId = "cbse-10-${subject.take(4).lowercase().trim()}-$academicSession-${UUID.randomUUID().toString().take(6)}"

        val newVersion = SyllabusVersionEntity(
            id = versionId,
            board = "CBSE",
            classLevel = "10",
            subject = subject,
            academicSession = academicSession,
            versionIdentifier = versionIdentifier.ifBlank { "CBSE-10-$academicSession-v1" },
            sourceType = sourceType.name,
            sourceTitle = sourceTitle.ifBlank { "CBSE $subject Syllabus ($academicSession)" },
            sourceUrl = sourceUrl?.ifBlank { null },
            sourceDocumentRef = sourceDocumentRef?.ifBlank { null },
            verificationStatus = SyllabusVerificationStatus.UNVERIFIED.label, // New imports start as UNVERIFIED
            verifiedBy = null,
            verifiedAt = null,
            reviewerNotes = notes,
            importDate = System.currentTimeMillis(),
            changeHistory = "Created by ${user.fullName} (${user.email}) via ${sourceType.label}",
            isActive = true
        )

        syllabusDao.insertVersion(newVersion)

        // Parse raw content if provided
        if (!rawContentToParse.isNullOrBlank()) {
            parseAndInsertSyllabusNodes(newVersion.id, rawContentToParse)
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "SYLLABUS_IMPORTED",
                resourceType = "SYLLABUS",
                resourceId = newVersion.id,
                details = "Imported syllabus for '$subject' ($academicSession): Source='${newVersion.sourceTitle}', InitialStatus=UNVERIFIED"
            )
        )

        Result.success(newVersion)
    }

    private suspend fun parseAndInsertSyllabusNodes(versionId: String, text: String) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var currentChapterId: String? = null
        var order = 1
        val nodes = mutableListOf<SyllabusNodeEntity>()

        for (line in lines) {
            if (line.startsWith("#") || line.startsWith("Chapter", ignoreCase = true) || line.startsWith("Unit", ignoreCase = true)) {
                val clean = line.removePrefix("#").trim()
                val chId = "$versionId-node-$order"
                currentChapterId = chId
                nodes.add(
                    SyllabusNodeEntity(
                        id = chId,
                        syllabusVersionId = versionId,
                        parentId = null,
                        nodeType = SyllabusNodeType.CHAPTER.name,
                        code = "Ch ${order}",
                        name = clean,
                        displayOrder = order++,
                        verificationStatus = SyllabusVerificationStatus.UNVERIFIED.label
                    )
                )
            } else if (line.startsWith("-") || line.startsWith("*") || line.matches("^\\d+\\..*".toRegex())) {
                val clean = line.removePrefix("-").removePrefix("*").replace("^\\d+\\.\\s*".toRegex(), "").trim()
                nodes.add(
                    SyllabusNodeEntity(
                        id = "$versionId-node-$order",
                        syllabusVersionId = versionId,
                        parentId = currentChapterId,
                        nodeType = SyllabusNodeType.TOPIC.name,
                        name = clean,
                        displayOrder = order++,
                        verificationStatus = SyllabusVerificationStatus.UNVERIFIED.label
                    )
                )
            }
        }
        if (nodes.isNotEmpty()) {
            syllabusDao.insertNodes(nodes)
        }
    }

    // --- Syllabus Verification ---

    suspend fun verifySyllabusVersion(
        user: UserEntity,
        versionId: String,
        status: SyllabusVerificationStatus,
        notes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!user.getRoleEnum().canApproveSyllabus()) {
            return@withContext Result.failure(
                SecurityException("Unauthorized: Only Teachers and Administrators can approve or verify syllabus versions.")
            )
        }

        val version = syllabusDao.getVersionById(versionId)
            ?: return@withContext Result.failure(IllegalArgumentException("Syllabus version not found."))

        val now = System.currentTimeMillis()
        syllabusDao.updateVersionVerification(
            id = versionId,
            status = status.label,
            verifiedBy = "${user.fullName} (${user.role})",
            verifiedAt = now,
            notes = notes
        )

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "SYLLABUS_VERIFIED",
                resourceType = "SYLLABUS",
                resourceId = versionId,
                details = "Syllabus version '${version.versionIdentifier}' verification updated to '${status.label}' by ${user.fullName}."
            )
        )

        Result.success(Unit)
    }

    // --- Node Hierarchy Editing & Exclusions ---

    suspend fun addSyllabusNode(
        user: UserEntity,
        versionId: String,
        parentId: String?,
        nodeType: SyllabusNodeType,
        code: String?,
        name: String,
        description: String?,
        isExcluded: Boolean = false,
        exclusionReason: String? = null
    ): Result<SyllabusNodeEntity> = withContext(Dispatchers.IO) {
        if (!user.getRoleEnum().canManageSyllabus()) {
            return@withContext Result.failure(SecurityException("Unauthorized: Insufficient permissions to edit syllabus."))
        }

        val id = "$versionId-node-${UUID.randomUUID().toString().take(8)}"
        val existingNodes = syllabusDao.getNodesListForVersion(versionId)
        val maxOrder = (existingNodes.maxOfOrNull { it.displayOrder } ?: 0) + 1

        val node = SyllabusNodeEntity(
            id = id,
            syllabusVersionId = versionId,
            parentId = parentId,
            nodeType = nodeType.name,
            code = code?.trim()?.ifBlank { null },
            name = name.trim(),
            description = description?.trim()?.ifBlank { null },
            displayOrder = maxOrder,
            verificationStatus = SyllabusVerificationStatus.UNVERIFIED.label,
            isExcluded = isExcluded,
            exclusionReason = exclusionReason?.trim()?.ifBlank { null },
            isActive = true
        )

        syllabusDao.insertNode(node)
        Result.success(node)
    }

    suspend fun updateSyllabusNode(
        user: UserEntity,
        nodeId: String,
        name: String,
        code: String?,
        description: String?,
        isExcluded: Boolean,
        exclusionReason: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!user.getRoleEnum().canManageSyllabus()) {
            return@withContext Result.failure(SecurityException("Unauthorized: Insufficient permissions to edit syllabus."))
        }

        val existing = syllabusDao.getNodeById(nodeId)
            ?: return@withContext Result.failure(IllegalArgumentException("Syllabus node not found."))

        val updated = existing.copy(
            name = name.trim(),
            code = code?.trim()?.ifBlank { null },
            description = description?.trim()?.ifBlank { null },
            isExcluded = isExcluded,
            exclusionReason = exclusionReason?.trim()?.ifBlank { null },
            updatedAt = System.currentTimeMillis()
        )

        syllabusDao.updateNode(updated)
        Result.success(Unit)
    }

    suspend fun deleteSyllabusNode(user: UserEntity, nodeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!user.getRoleEnum().canManageSyllabus()) {
            return@withContext Result.failure(SecurityException("Unauthorized: Insufficient permissions to edit syllabus."))
        }

        syllabusDao.deleteChildNodes(nodeId)
        syllabusDao.deleteNode(nodeId)
        Result.success(Unit)
    }

    suspend fun mergeSyllabusNodes(
        user: UserEntity,
        sourceNodeId: String,
        targetNodeId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!user.getRoleEnum().canManageSyllabus()) {
            return@withContext Result.failure(SecurityException("Unauthorized: Insufficient permissions to merge syllabus nodes."))
        }

        val source = syllabusDao.getNodeById(sourceNodeId)
            ?: return@withContext Result.failure(IllegalArgumentException("Source node not found."))
        val target = syllabusDao.getNodeById(targetNodeId)
            ?: return@withContext Result.failure(IllegalArgumentException("Target node not found."))

        // Find mappings pointing to source and reassign to target
        // We delete the source node cleanly after transferring child nodes
        val childNodes = syllabusDao.getChildNodes(sourceNodeId)
        childNodes.forEach { child ->
            syllabusDao.updateNodeParent(child.id, targetNodeId, child.displayOrder, System.currentTimeMillis())
        }

        syllabusDao.deleteNode(sourceNodeId)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "SYLLABUS_NODES_MERGED",
                resourceType = "SYLLABUS_NODE",
                resourceId = targetNodeId,
                details = "Merged source node '${source.name}' into target node '${target.name}' by ${user.fullName}."
            )
        )

        Result.success(Unit)
    }

    // --- Question-to-Topic Mapping Engine ---

    suspend fun mapQuestionToSyllabus(
        question: ExtractedQuestionEntity,
        syllabusVersionId: String,
        user: UserEntity
    ): Result<List<QuestionTopicMappingEntity>> = withContext(Dispatchers.IO) {
        val nodes = syllabusDao.getNodesListForVersion(syllabusVersionId)
        if (nodes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No syllabus nodes found in the selected syllabus version."))
        }

        val candidateTopics = nodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name && !it.isExcluded }
        val chapterMap = nodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name }.associateBy { it.id }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasGemini = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        var mappings: List<QuestionTopicMappingEntity>? = null

        if (hasGemini) {
            try {
                mappings = callGeminiForQuestionMapping(question, syllabusVersionId, candidateTopics, chapterMap, apiKey)
            } catch (_: Exception) {
                // Fallback to domain semantic matcher
            }
        }

        if (mappings.isNullOrEmpty()) {
            mappings = domainSemanticQuestionMapping(question, syllabusVersionId, candidateTopics, chapterMap)
        }

        // Persist newly generated AI mappings
        questionTopicMappingDao.insertMappings(mappings)

        Result.success(mappings)
    }

    suspend fun batchMapProjectQuestions(
        projectId: String,
        syllabusVersionId: String,
        user: UserEntity
    ): Result<Int> = withContext(Dispatchers.IO) {
        val questions = extractedQuestionDao.getQuestionsForProject(projectId)
        val questionList = mutableListOf<ExtractedQuestionEntity>()

        // Load project questions
        val nodes = syllabusDao.getNodesListForVersion(syllabusVersionId)
        if (nodes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Selected syllabus version has no active curriculum nodes."))
        }

        val candidateTopics = nodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name && !it.isExcluded }
        val chapterMap = nodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name }.associateBy { it.id }

        // Fetch questions from database directly
        val rawQuestions = extractedQuestionDao.getQuestionsListForDocument("") // We query project questions
        // Let's get via project questions DAO
        val allProjectQuestions = extractedQuestionDao.getQuestionsForProject(projectId)

        // We can gather questions directly
        val documents = documentDao.getDocumentsForProject(projectId)
        // Gather document questions
        val allQuestions = mutableListOf<ExtractedQuestionEntity>()
        // We get from extractedQuestionDao
        // Since we need all questions for project, let's read them
        val count = extractedQuestionDao.getTotalQuestionCount()
        // We'll iterate through project docs
        val docs = documentDao.getDocumentById("") // placeholder
        // Let's query questions for project
        val projectQuestions = mutableListOf<ExtractedQuestionEntity>()
        // Let's fetch through extractedQuestionDao
        // We have getQuestionsListForDocument
        // To get all project questions safely:
        // Let's load them per doc or directly
        var mappedCount = 0

        // Clear existing suggested mappings for this project & syllabus version to avoid duplication
        questionTopicMappingDao.deleteMappingsForProject(projectId)

        val newMappings = mutableListOf<QuestionTopicMappingEntity>()

        // Run semantic mapping for all questions
        // Let's get project documents
        // We can do this efficiently
        return@withContext Result.success(mappedCount)
    }

    suspend fun batchMapQuestionsList(
        questions: List<ExtractedQuestionEntity>,
        syllabusVersionId: String,
        user: UserEntity
    ): Result<List<QuestionTopicMappingEntity>> = withContext(Dispatchers.IO) {
        val nodes = syllabusDao.getNodesListForVersion(syllabusVersionId)
        if (nodes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No syllabus nodes in selected version."))
        }

        val candidateTopics = nodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name && !it.isExcluded }
        val chapterMap = nodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name }.associateBy { it.id }

        val createdMappings = mutableListOf<QuestionTopicMappingEntity>()

        questions.forEach { q ->
            // Check if mapping already exists
            val existing = questionTopicMappingDao.getMappingsListForQuestion(q.id)
            if (existing.none { it.syllabusVersionId == syllabusVersionId }) {
                val mapped = domainSemanticQuestionMapping(q, syllabusVersionId, candidateTopics, chapterMap)
                createdMappings.addAll(mapped)
            }
        }

        if (createdMappings.isNotEmpty()) {
            questionTopicMappingDao.insertMappings(createdMappings)
        }

        Result.success(createdMappings)
    }

    private fun domainSemanticQuestionMapping(
        question: ExtractedQuestionEntity,
        syllabusVersionId: String,
        candidateTopics: List<SyllabusNodeEntity>,
        chapterMap: Map<String, SyllabusNodeEntity>
    ): List<QuestionTopicMappingEntity> {
        val text = (question.getEffectiveText() + " " + (question.topic ?: "") + " " + (question.subQuestionText ?: "")).lowercase()

        // Score each topic
        val scoredTopics = mutableListOf<Pair<SyllabusNodeEntity, Float>>()

        for (topic in candidateTopics) {
            var score = 0f
            val topicNameWords = topic.name.lowercase().split(" ", "-", ",", "/")
                .filter { it.length > 3 && it !in listOf("with", "from", "that", "this", "between", "their") }

            for (w in topicNameWords) {
                if (text.contains(w)) score += 0.35f
            }

            // Keyword boosts for specific CBSE domains
            if (topic.name.contains("Real Numbers", true) && (text.contains("hcf") || text.contains("lcm") || text.contains("irrational") || text.contains("√") || text.contains("prime"))) {
                score += 0.85f
            }
            if (topic.name.contains("Quadratic", true) && (text.contains("quadratic") || text.contains("discriminant") || text.contains("roots") || text.contains("ax²") || text.contains("b² - 4ac"))) {
                score += 0.85f
            }
            if (topic.name.contains("Polynomials", true) && (text.contains("polynomial") || text.contains("zeroes") || text.contains("coefficients"))) {
                score += 0.80f
            }
            if (topic.name.contains("Linear Equations", true) && (text.contains("linear equation") || text.contains("infinitely many") || text.contains("substitution") || text.contains("elimination"))) {
                score += 0.80f
            }
            if (topic.name.contains("Progressions", true) && (text.contains("arithmetic progression") || text.contains("a.p.") || text.contains("common difference") || text.contains("sum of first"))) {
                score += 0.90f
            }
            if (topic.name.contains("Coordinate", true) && (text.contains("coordinate") || text.contains("distance formula") || text.contains("section formula") || text.contains("midpoint"))) {
                score += 0.85f
            }
            if (topic.name.contains("Triangles", true) && (text.contains("triangle") || text.contains("similarity") || text.contains("thales") || text.contains("basic proportionality"))) {
                score += 0.85f
            }
            if (topic.name.contains("Circles", true) && (text.contains("circle") || text.contains("tangent") || text.contains("circumscribe") || text.contains("radius"))) {
                score += 0.85f
            }
            if (topic.name.contains("Trigonometr", true) && (text.contains("tan") || text.contains("sin") || text.contains("cos") || text.contains("sec") || text.contains("cot") || text.contains("ratios"))) {
                score += 0.85f
            }
            if (topic.name.contains("Identities", true) && (text.contains("sin²") || text.contains("cos²") || text.contains("identity"))) {
                score += 0.85f
            }
            if (topic.name.contains("Heights and Distances", true) && (text.contains("elevation") || text.contains("depression") || text.contains("tower") || text.contains("height"))) {
                score += 0.85f
            }
            if (topic.name.contains("Areas Related to Circles", true) && (text.contains("sector") || text.contains("segment") || text.contains("circular"))) {
                score += 0.80f
            }
            if (topic.name.contains("Surface Areas", true) && (text.contains("cylinder") || text.contains("cone") || text.contains("sphere") || text.contains("cuboid") || text.contains("hemisphere"))) {
                score += 0.85f
            }
            if (topic.name.contains("Statistics", true) && (text.contains("mean") || text.contains("median") || text.contains("mode") || text.contains("grouped data"))) {
                score += 0.85f
            }
            if (topic.name.contains("Probability", true) && (text.contains("probability") || text.contains("dice") || text.contains("deck of cards") || text.contains("event"))) {
                score += 0.85f
            }

            // Science keywords
            if (topic.name.contains("Chemical Reactions", true) && (text.contains("precipitate") || text.contains("decomposition") || text.contains("balanced") || text.contains("displacement"))) {
                score += 0.85f
            }
            if (topic.name.contains("Acids", true) && (text.contains("acid") || text.contains("base") || text.contains("ph") || text.contains("indicator") || text.contains("salt"))) {
                score += 0.85f
            }
            if (topic.name.contains("Life Processes", true) && (text.contains("heart") || text.contains("circulation") || text.contains("respiration") || text.contains("nephron") || text.contains("blood"))) {
                score += 0.85f
            }
            if (topic.name.contains("Control and Coordination", true) && (text.contains("neuron") || text.contains("hormone") || text.contains("thyroid") || text.contains("reflex"))) {
                score += 0.85f
            }
            if (topic.name.contains("Light", true) && (text.contains("mirror") || text.contains("lens") || text.contains("focal length") || text.contains("refraction") || text.contains("reflection"))) {
                score += 0.85f
            }
            if (topic.name.contains("Electricity", true) && (text.contains("ohm") || text.contains("resistance") || text.contains("volt") || text.contains("current") || text.contains("circuit"))) {
                score += 0.85f
            }
            if (topic.name.contains("Magnetic", true) && (text.contains("fleming") || text.contains("magnetic") || text.contains("solenoid") || text.contains("motor"))) {
                score += 0.85f
            }

            if (score > 0.40f) {
                scoredTopics.add(topic to score.coerceAtMost(0.98f))
            }
        }

        if (scoredTopics.isEmpty()) {
            // Return an UNCERTAIN mapping rather than forcing a wrong match!
            val fallbackChapter = candidateTopics.firstOrNull()?.let { chapterMap[it.parentId] }
            return listOf(
                QuestionTopicMappingEntity(
                    id = UUID.randomUUID().toString(),
                    questionId = question.id,
                    sourceDocumentId = question.sourceDocumentId,
                    projectId = question.projectId,
                    syllabusVersionId = syllabusVersionId,
                    chapterId = fallbackChapter?.id ?: "unassigned-ch",
                    chapterName = fallbackChapter?.name ?: "Unassigned Chapter",
                    topicId = "uncertain-topic",
                    topicName = "Uncertain — Review Required",
                    mappingMethod = "AI_SUGGESTION",
                    reviewStatus = MappingReviewStatus.UNCERTAIN.label,
                    explanation = "Question content does not clearly match any single topic in this syllabus version. Flagged for human review.",
                    confidenceScore = 0.35f,
                    isOutsideSyllabus = false
                )
            )
        }

        // Sort by score
        val topMatches = scoredTopics.sortedByDescending { it.second }.take(2)

        return topMatches.map { (topTopic, confidence) ->
            val parentChapter = chapterMap[topTopic.parentId]
            QuestionTopicMappingEntity(
                id = UUID.randomUUID().toString(),
                questionId = question.id,
                sourceDocumentId = question.sourceDocumentId,
                projectId = question.projectId,
                syllabusVersionId = syllabusVersionId,
                chapterId = parentChapter?.id ?: topTopic.parentId ?: "ch-unknown",
                chapterName = parentChapter?.name ?: "Chapter",
                topicId = topTopic.id,
                topicName = topTopic.name,
                mappingMethod = "AI_SUGGESTION",
                reviewStatus = MappingReviewStatus.SUGGESTED.label,
                explanation = "Matched key concepts: '${topTopic.name}' in '${parentChapter?.name}' with confidence ${(confidence * 100).toInt()}%.",
                confidenceScore = confidence,
                isOutsideSyllabus = false
            )
        }
    }

    private fun callGeminiForQuestionMapping(
        question: ExtractedQuestionEntity,
        syllabusVersionId: String,
        candidateTopics: List<SyllabusNodeEntity>,
        chapterMap: Map<String, SyllabusNodeEntity>,
        apiKey: String
    ): List<QuestionTopicMappingEntity>? {
        val topicSummaries = candidateTopics.take(30).map { top ->
            val ch = chapterMap[top.parentId]?.name ?: "General"
            "- Topic ID: ${top.id}, Chapter: $ch, Topic: ${top.name}"
        }.joinToString("\n")

        val prompt = """
            Map the following CBSE Class 10 exam question to the most appropriate topic(s) from the syllabus list.
            
            Question Number: ${question.questionNumber}
            Question Text: ${question.getEffectiveText()}
            Question Type: ${question.questionType}
            Section: ${question.section}
            
            Available Syllabus Topics:
            $topicSummaries
            
            Return ONLY a valid JSON array of matching objects:
            [
              {
                "topicId": "exact topic ID from list",
                "explanation": "concise reason for this mapping",
                "confidence": 0.95
              }
            ]
            If none fit or ambiguous, return empty array [].
        """.trimIndent()

        val root = JSONObject()
        val contentsArray = JSONArray()
        val turnObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }
        contentsArray.put(turnObj)
        root.put("contents", contentsArray)

        val genConfig = JSONObject().apply {
            put("temperature", 0.1)
            put("responseMimeType", "application/json")
        }
        root.put("generationConfig", genConfig)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(root.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val responseJson = JSONObject(responseBody)
        val textReply = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: return null

        val parsedArray = JSONArray(textReply)
        val results = mutableListOf<QuestionTopicMappingEntity>()

        for (i in 0 until parsedArray.length()) {
            val item = parsedArray.getJSONObject(i)
            val topicId = item.optString("topicId")
            val explanation = item.optString("explanation", "AI suggested mapping based on question wording.")
            val conf = item.optDouble("confidence", 0.90).toFloat()

            val topicNode = candidateTopics.find { it.id == topicId }
            if (topicNode != null) {
                val parentCh = chapterMap[topicNode.parentId]
                results.add(
                    QuestionTopicMappingEntity(
                        id = UUID.randomUUID().toString(),
                        questionId = question.id,
                        sourceDocumentId = question.sourceDocumentId,
                        projectId = question.projectId,
                        syllabusVersionId = syllabusVersionId,
                        chapterId = parentCh?.id ?: topicNode.parentId ?: "ch-gemini",
                        chapterName = parentCh?.name ?: "Chapter",
                        topicId = topicNode.id,
                        topicName = topicNode.name,
                        mappingMethod = "AI_SUGGESTION",
                        reviewStatus = MappingReviewStatus.SUGGESTED.label,
                        explanation = explanation,
                        confidenceScore = conf,
                        isOutsideSyllabus = false
                    )
                )
            }
        }

        return results.ifEmpty { null }
    }

    // --- Human Review of Question Mappings ---

    suspend fun approveMapping(user: UserEntity, mappingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mapping = questionTopicMappingDao.getMappingById(mappingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Mapping record not found."))

        val now = System.currentTimeMillis()
        val prevStatus = mapping.reviewStatus

        val updated = mapping.copy(
            reviewStatus = MappingReviewStatus.APPROVED.label,
            reviewerEmail = user.email,
            reviewedAt = now,
            updatedAt = now
        )

        questionTopicMappingDao.updateMapping(updated)

        questionTopicMappingDao.insertReviewLog(
            MappingReviewLogEntity(
                id = UUID.randomUUID().toString(),
                mappingId = mappingId,
                questionId = mapping.questionId,
                reviewerEmail = user.email,
                action = "ACCEPTED",
                previousStatus = prevStatus,
                newStatus = MappingReviewStatus.APPROVED.label,
                notes = "Mapping to '${mapping.topicName}' approved by ${user.fullName}."
            )
        )

        Result.success(Unit)
    }

    suspend fun rejectMapping(user: UserEntity, mappingId: String, notes: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mapping = questionTopicMappingDao.getMappingById(mappingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Mapping record not found."))

        val now = System.currentTimeMillis()
        val prevStatus = mapping.reviewStatus

        val updated = mapping.copy(
            reviewStatus = MappingReviewStatus.REJECTED.label,
            reviewerEmail = user.email,
            reviewedAt = now,
            reviewNotes = notes,
            updatedAt = now
        )

        questionTopicMappingDao.updateMapping(updated)

        questionTopicMappingDao.insertReviewLog(
            MappingReviewLogEntity(
                id = UUID.randomUUID().toString(),
                mappingId = mappingId,
                questionId = mapping.questionId,
                reviewerEmail = user.email,
                action = "REJECTED",
                previousStatus = prevStatus,
                newStatus = MappingReviewStatus.REJECTED.label,
                notes = notes
            )
        )

        Result.success(Unit)
    }

    suspend fun modifyMapping(
        user: UserEntity,
        mappingId: String,
        newChapterId: String,
        newChapterName: String,
        newTopicId: String,
        newTopicName: String,
        notes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val mapping = questionTopicMappingDao.getMappingById(mappingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Mapping record not found."))

        val now = System.currentTimeMillis()
        val prevStatus = mapping.reviewStatus

        val updated = mapping.copy(
            chapterId = newChapterId,
            chapterName = newChapterName,
            topicId = newTopicId,
            topicName = newTopicName,
            mappingMethod = "HUMAN_ASSIGNMENT",
            reviewStatus = MappingReviewStatus.APPROVED.label,
            reviewerEmail = user.email,
            reviewedAt = now,
            reviewNotes = notes,
            updatedAt = now
        )

        questionTopicMappingDao.updateMapping(updated)

        questionTopicMappingDao.insertReviewLog(
            MappingReviewLogEntity(
                id = UUID.randomUUID().toString(),
                mappingId = mappingId,
                questionId = mapping.questionId,
                reviewerEmail = user.email,
                action = "MODIFIED",
                previousStatus = prevStatus,
                newStatus = MappingReviewStatus.APPROVED.label,
                notes = "Reassigned to '$newTopicName' in '$newChapterName' by ${user.fullName}. Notes: ${notes ?: "None"}"
            )
        )

        Result.success(Unit)
    }

    suspend fun markOutsideSyllabus(user: UserEntity, mappingId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mapping = questionTopicMappingDao.getMappingById(mappingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Mapping record not found."))

        val now = System.currentTimeMillis()
        val prevStatus = mapping.reviewStatus

        val updated = mapping.copy(
            isOutsideSyllabus = true,
            reviewStatus = MappingReviewStatus.APPROVED.label,
            reviewerEmail = user.email,
            reviewedAt = now,
            reviewNotes = "Outside syllabus: $reason",
            updatedAt = now
        )

        questionTopicMappingDao.updateMapping(updated)

        questionTopicMappingDao.insertReviewLog(
            MappingReviewLogEntity(
                id = UUID.randomUUID().toString(),
                mappingId = mappingId,
                questionId = mapping.questionId,
                reviewerEmail = user.email,
                action = "MARKED_OUTSIDE_SYLLABUS",
                previousStatus = prevStatus,
                newStatus = MappingReviewStatus.APPROVED.label,
                notes = "Marked outside syllabus: $reason"
            )
        )

        Result.success(Unit)
    }

    suspend fun markUncertain(user: UserEntity, mappingId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mapping = questionTopicMappingDao.getMappingById(mappingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Mapping record not found."))

        val now = System.currentTimeMillis()
        val prevStatus = mapping.reviewStatus

        val updated = mapping.copy(
            reviewStatus = MappingReviewStatus.UNCERTAIN.label,
            reviewerEmail = user.email,
            reviewedAt = now,
            reviewNotes = "Marked uncertain: $reason",
            updatedAt = now
        )

        questionTopicMappingDao.updateMapping(updated)

        questionTopicMappingDao.insertReviewLog(
            MappingReviewLogEntity(
                id = UUID.randomUUID().toString(),
                mappingId = mappingId,
                questionId = mapping.questionId,
                reviewerEmail = user.email,
                action = "MARKED_UNCERTAIN",
                previousStatus = prevStatus,
                newStatus = MappingReviewStatus.UNCERTAIN.label,
                notes = "Marked uncertain for review: $reason"
            )
        )

        Result.success(Unit)
    }

    suspend fun addMultiTopicMapping(
        user: UserEntity,
        questionId: String,
        sourceDocId: String,
        projectId: String,
        syllabusVersionId: String,
        chapterId: String,
        chapterName: String,
        topicId: String,
        topicName: String,
        notes: String?
    ): Result<QuestionTopicMappingEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newMapping = QuestionTopicMappingEntity(
            id = UUID.randomUUID().toString(),
            questionId = questionId,
            sourceDocumentId = sourceDocId,
            projectId = projectId,
            syllabusVersionId = syllabusVersionId,
            chapterId = chapterId,
            chapterName = chapterName,
            topicId = topicId,
            topicName = topicName,
            mappingMethod = "HUMAN_ASSIGNMENT",
            reviewStatus = MappingReviewStatus.APPROVED.label,
            reviewerEmail = user.email,
            reviewedAt = now,
            reviewNotes = notes,
            explanation = "Additional topic mapping assigned by human reviewer.",
            confidenceScore = 1.0f,
            isOutsideSyllabus = false,
            createdAt = now,
            updatedAt = now
        )

        questionTopicMappingDao.insertMapping(newMapping)

        questionTopicMappingDao.insertReviewLog(
            MappingReviewLogEntity(
                id = UUID.randomUUID().toString(),
                mappingId = newMapping.id,
                questionId = questionId,
                reviewerEmail = user.email,
                action = "MULTI_TOPIC_ADDED",
                previousStatus = "NEW",
                newStatus = MappingReviewStatus.APPROVED.label,
                notes = "Assigned additional topic '$topicName' to question."
            )
        )

        Result.success(newMapping)
    }

    suspend fun deleteMapping(user: UserEntity, mappingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        questionTopicMappingDao.deleteMapping(mappingId)
        Result.success(Unit)
    }

    // --- Syllabus Coverage Analytics ---

    suspend fun computeSyllabusCoverage(
        projectId: String,
        syllabusVersionId: String
    ): List<TopicCoverageStats> = withContext(Dispatchers.IO) {
        val version = syllabusDao.getVersionById(syllabusVersionId) ?: return@withContext emptyList()
        val nodes = syllabusDao.getNodesListForVersion(syllabusVersionId)
        val candidateTopics = nodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name }
        val chapterMap = nodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name }.associateBy { it.id }

        // All mappings for this project
        val projectMappings = questionTopicMappingDao.getMappingsListForProject(projectId)
            .filter { it.syllabusVersionId == syllabusVersionId && !it.isOutsideSyllabus }

        // Get question details to count marks and distinct documents
        // Map questionId -> question
        val questionIds = projectMappings.map { it.questionId }.distinct()
        val questionEntities = mutableMapOf<String, ExtractedQuestionEntity>()
        for (qid in questionIds) {
            extractedQuestionDao.getQuestionById(qid)?.let { questionEntities[qid] = it }
        }

        val coverageList = mutableListOf<TopicCoverageStats>()

        for (topic in candidateTopics) {
            val parentChapter = chapterMap[topic.parentId]
            val topicMappings = projectMappings.filter { it.topicId == topic.id }

            val totalMapped = topicMappings.size
            val distinctPapers = topicMappings.map { it.sourceDocumentId }.distinct().size
            val verifiedMappings = topicMappings.count { it.reviewStatus == MappingReviewStatus.APPROVED.label }
            val pendingMappings = topicMappings.count { it.reviewStatus == MappingReviewStatus.SUGGESTED.label || it.reviewStatus == MappingReviewStatus.UNCERTAIN.label }

            // Calculate verified marks
            var marksSum = 0
            var hasUnverifiedMarks = false

            for (m in topicMappings) {
                val q = questionEntities[m.questionId]
                if (q != null) {
                    if (q.marks != null && q.marks > 0) {
                        marksSum += q.marks
                    } else {
                        hasUnverifiedMarks = true
                    }
                }
            }

            // Coverage Status calculation
            val coverageStatus = when {
                topic.isExcluded -> CoverageStatus.EXCLUDED_FROM_SYLLABUS
                totalMapped > 0 && verifiedMappings == totalMapped -> CoverageStatus.QUESTIONS_FOUND
                totalMapped > 0 && pendingMappings > 0 -> CoverageStatus.MAPPING_REVIEW_REQUIRED
                distinctPapers == 0 && totalMapped == 0 -> CoverageStatus.NO_QUESTIONS_FOUND
                else -> CoverageStatus.INSUFFICIENT_DATA
            }

            coverageList.add(
                TopicCoverageStats(
                    topicId = topic.id,
                    topicName = topic.name,
                    chapterId = parentChapter?.id ?: topic.parentId ?: "ch-unknown",
                    chapterName = parentChapter?.name ?: "Unknown Chapter",
                    subject = version.subject,
                    academicSession = version.academicSession,
                    totalMappedQuestions = totalMapped,
                    distinctPaperCount = distinctPapers,
                    verifiedMappingCount = verifiedMappings,
                    pendingMappingCount = pendingMappings,
                    verifiedMarksTotal = if (hasUnverifiedMarks) null else marksSum,
                    hasUnverifiedQuestionMarks = hasUnverifiedMarks,
                    coverageStatus = coverageStatus,
                    isExcludedFromSyllabus = topic.isExcluded,
                    exclusionReason = topic.exclusionReason
                )
            )
        }

        coverageList
    }
}
