package com.auditreports.service;

import com.auditreports.model.ReportState;
import com.auditreports.model.StateTransition;
import com.auditreports.repository.AuditReportRepository;
import com.auditreports.repository.ReportStateRepository;
import com.auditreports.repository.StateTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StateConfigService {

    private final ReportStateRepository stateRepository;
    private final StateTransitionRepository transitionRepository;
    private final AuditReportRepository reportRepository;

    public StateConfigService(ReportStateRepository stateRepository,
                              StateTransitionRepository transitionRepository,
                              AuditReportRepository reportRepository) {
        this.stateRepository = stateRepository;
        this.transitionRepository = transitionRepository;
        this.reportRepository = reportRepository;
    }

    public List<ReportState> getAllStates() {
        return stateRepository.findAllByOrderByDisplayOrderAsc();
    }

    public ReportState getState(Long id) {
        return stateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("State not found: " + id));
    }

    public ReportState createState(ReportState state) {
        if (stateRepository.findByName(state.getName()).isPresent()) {
            throw new IllegalArgumentException("State with name '" + state.getName() + "' already exists");
        }
        return stateRepository.save(state);
    }

    public ReportState updateState(Long id, ReportState updated) {
        ReportState existing = getState(id);
        existing.setName(updated.getName());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setIsInitial(updated.getIsInitial());
        existing.setIsTerminal(updated.getIsTerminal());
        return stateRepository.save(existing);
    }

    public void deleteState(Long id) {
        ReportState state = getState(id);
        if (reportRepository.existsByCurrentState(state)) {
            throw new IllegalStateException("Cannot delete state that is in use by reports");
        }
        if (transitionRepository.existsByFromStateOrToState(state, state)) {
            throw new IllegalStateException("Cannot delete state that is referenced in transitions. Remove transitions first.");
        }
        stateRepository.delete(state);
    }

    public List<StateTransition> getAllTransitions() {
        return transitionRepository.findAll();
    }

    public StateTransition createTransition(Long fromStateId, Long toStateId) {
        ReportState fromState = getState(fromStateId);
        ReportState toState = getState(toStateId);
        if (transitionRepository.findByFromStateAndToState(fromState, toState).isPresent()) {
            throw new IllegalArgumentException("Transition already exists");
        }
        return transitionRepository.save(new StateTransition(fromState, toState));
    }

    public void deleteTransition(Long id) {
        if (!transitionRepository.existsById(id)) {
            throw new IllegalArgumentException("Transition not found: " + id);
        }
        transitionRepository.deleteById(id);
    }

    public List<StateTransition> getTransitionsFrom(ReportState state) {
        return transitionRepository.findByFromState(state);
    }
}
