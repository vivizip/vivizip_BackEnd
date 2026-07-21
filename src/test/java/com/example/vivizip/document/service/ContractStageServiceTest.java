package com.example.vivizip.document.service;

import com.example.vivizip.document.entity.AnalysisType;
import com.example.vivizip.document.entity.ContractStage;
import com.example.vivizip.document.entity.DocumentAnalysis;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.entity.LeaseDocumentType;
import com.example.vivizip.document.repository.DocumentAnalysisRepository;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ContractStageServiceTest {

    @Mock
    private LeaseCaseRepository leaseCaseRepository;
    @Mock
    private LeaseDocumentRepository leaseDocumentRepository;
    @Mock
    private DocumentAnalysisRepository documentAnalysisRepository;

    private ContractStageService contractStageService;
    private LeaseCase leaseCase;

    @BeforeEach
    void setUp() {
        contractStageService = new ContractStageService(leaseCaseRepository, leaseDocumentRepository, documentAnalysisRepository);
        leaseCase = LeaseCase.create(1L, "우리집", "서울시 강남구 테헤란로", null);
        ReflectionTestUtils.setField(leaseCase, "id", 100L);
    }

    @Test
    void 아무_서류도_분석되지_않았으면_계약_전_단계를_유지한다() {
        givenAnalysis(LeaseDocumentType.REGISTRY, null);
        givenAnalysis(LeaseDocumentType.BUILDING_LEDGER, null);
        givenAnalysis(LeaseDocumentType.BROKERAGE_CONFIRMATION, null);

        ContractStage stage = contractStageService.computeStage(leaseCase);

        assertThat(stage).isEqualTo(ContractStage.BEFORE_CONTRACT);
    }

    @Test
    void 등기부등본만_완료되면_계약_전_단계를_유지한다() {
        givenAnalysis(LeaseDocumentType.REGISTRY, AnalysisType.REGISTRY_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BUILDING_LEDGER, null);
        givenAnalysis(LeaseDocumentType.BROKERAGE_CONFIRMATION, null);

        ContractStage stage = contractStageService.computeStage(leaseCase);

        assertThat(stage).isEqualTo(ContractStage.BEFORE_CONTRACT);
    }

    @Test
    void 등기부등본과_건축물대장이_모두_완료되면_계약_중_단계가_된다() {
        givenAnalysis(LeaseDocumentType.REGISTRY, AnalysisType.REGISTRY_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BUILDING_LEDGER, AnalysisType.BUILDING_LEDGER_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BROKERAGE_CONFIRMATION, null);

        ContractStage stage = contractStageService.computeStage(leaseCase);

        assertThat(stage).isEqualTo(ContractStage.DURING_CONTRACT);
    }

    @Test
    void 중개대상물_확인설명서와_임대차계약서_분석까지_끝나면_계약_후_단계가_된다() {
        givenAnalysis(LeaseDocumentType.REGISTRY, AnalysisType.REGISTRY_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BUILDING_LEDGER, AnalysisType.BUILDING_LEDGER_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BROKERAGE_CONFIRMATION, AnalysisType.BROKERAGE_DOCUMENT_ANALYSIS);
        // 임대차계약서는 document_analysis에 결과를 남기지 않고 LeaseCase.status(COMPLETED)로만 완료를 표시한다.
        leaseCase.complete();

        ContractStage stage = contractStageService.computeStage(leaseCase);

        assertThat(stage).isEqualTo(ContractStage.AFTER_CONTRACT);
    }

    @Test
    void 중개대상물_확인설명서는_끝났지만_임대차계약서_분석이_아직이면_계약_중에_머문다() {
        givenAnalysis(LeaseDocumentType.REGISTRY, AnalysisType.REGISTRY_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BUILDING_LEDGER, AnalysisType.BUILDING_LEDGER_ANALYSIS);
        givenAnalysis(LeaseDocumentType.BROKERAGE_CONFIRMATION, AnalysisType.BROKERAGE_DOCUMENT_ANALYSIS);
        // leaseCase.complete() 호출하지 않음 -> 아직 ACTIVE (임대차계약서 분석 미완료로 간주)

        ContractStage stage = contractStageService.computeStage(leaseCase);

        assertThat(stage).isEqualTo(ContractStage.DURING_CONTRACT);
    }

    @Test
    void refreshStage는_이미_올라간_단계를_계산값이_낮아도_역행시키지_않는다() {
        leaseCase.advanceContractStage(ContractStage.DURING_CONTRACT);
        leaseCase.advanceContractStage(ContractStage.AFTER_CONTRACT);
        lenient().when(leaseCaseRepository.findById(100L)).thenReturn(Optional.of(leaseCase));
        givenAnalysis(LeaseDocumentType.REGISTRY, null);
        givenAnalysis(LeaseDocumentType.BUILDING_LEDGER, null);
        givenAnalysis(LeaseDocumentType.BROKERAGE_CONFIRMATION, null);

        contractStageService.refreshStage(100L);

        assertThat(leaseCase.getContractStage()).isEqualTo(ContractStage.AFTER_CONTRACT);
    }

    private void givenAnalysis(LeaseDocumentType documentType, AnalysisType completedAnalysisType) {
        LeaseDocument document = LeaseDocument.create(leaseCase.getId(), documentType);
        ReflectionTestUtils.setField(document, "id", (long) (documentType.ordinal() + 1));
        lenient().when(leaseDocumentRepository.findFirstByLeaseCaseIdAndDocumentTypeOrderByIdDesc(leaseCase.getId(), documentType))
                .thenReturn(Optional.of(document));

        if (completedAnalysisType == null) {
            lenient().when(documentAnalysisRepository.findFirstByDocumentIdOrderByIdDesc(document.getId()))
                    .thenReturn(Optional.empty());
            return;
        }

        DocumentAnalysis analysis = DocumentAnalysis.create(document.getId(), completedAnalysisType);
        analysis.start();
        analysis.complete("{}", null);
        lenient().when(documentAnalysisRepository.findFirstByDocumentIdOrderByIdDesc(document.getId()))
                .thenReturn(Optional.of(analysis));
    }
}
