package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.dto.CriterionScoreInput;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CriterionScoreInputDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.CriterionScoreResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.FeedbackResponseDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class FeedbackRestMapper {

    private FeedbackRestMapper() {
    }

    public static FeedbackResponseDTO toResponse(Feedback domain) {
        if (domain == null) {
            return null;
        }
        FeedbackResponseDTO response = new FeedbackResponseDTO();
        response.setId(domain.getId());
        response.setComment(domain.getComment());
        response.setCreatedAt(domain.getCreatedAt());

        if (domain.getSubmittedBy() != null) {
            response.setSubmittedById(domain.getSubmittedBy().getId());
            response.setSubmittedByName(
                    safeName(domain.getSubmittedBy().getFirstName(),
                            domain.getSubmittedBy().getLastName()));
            response.setSubmittedByProfilePictureUrl(domain.getSubmittedBy().getProfilePictureUrl());
            if (domain.getSubmittedBy().getRole() != null) {
                response.setSubmittedByRole(domain.getSubmittedBy().getRole().toString());
            }
        }

        if (domain.getAttention() != null) {
            response.setAttentionId(domain.getAttention().getId());
            if (domain.getAttention().getTreatment() != null) {
                response.setTreatmentName(domain.getAttention().getTreatment().getName());
            }
            if (domain.getAttention().getPatient() != null
                    && domain.getAttention().getPatient().getUser() != null) {
                var p = domain.getAttention().getPatient().getUser();
                response.setPatientName(safeName(p.getFirstName(), p.getLastName()));
                response.setPatientProfilePictureUrl(p.getProfilePictureUrl());
            }
            if (domain.getAttention().getPractitioner() != null
                    && domain.getAttention().getPractitioner().getUser() != null) {
                var pr = domain.getAttention().getPractitioner().getUser();
                response.setPractitionerName(safeName(pr.getFirstName(), pr.getLastName()));
                response.setPractitionerProfilePictureUrl(pr.getProfilePictureUrl());
            }
        }

        if (domain.getScores() != null && !domain.getScores().isEmpty()) {
            response.setScores(domain.getScores().stream()
                    .filter(s -> s.getCriterion() != null)
                    .map(s -> new CriterionScoreResponseDTO(
                            s.getCriterion().getCode(),
                            s.getCriterion().getDisplayName(),
                            s.getScore()))
                    .collect(Collectors.toList()));
        } else {
            response.setScores(Collections.emptyList());
        }
        return response;
    }

    public static List<CriterionScoreInput> toCommand(List<CriterionScoreInputDTO> scores) {
        if (scores == null) {
            return Collections.emptyList();
        }
        return scores.stream()
                .map(s -> new CriterionScoreInput(s.getCriterionCode(), s.getScore()))
                .collect(Collectors.toList());
    }

    private static String safeName(String firstName, String lastName) {
        String f = firstName == null ? "" : firstName;
        String l = lastName == null ? "" : lastName;
        return (f + " " + l).trim();
    }
}
