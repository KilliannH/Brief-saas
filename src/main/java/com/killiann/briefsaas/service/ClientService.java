package com.killiann.briefsaas.service;

import com.killiann.briefsaas.dto.ClientDto;
import com.killiann.briefsaas.entity.Client;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.ForbiddenException;
import com.killiann.briefsaas.exception.NotFoundException;
import com.killiann.briefsaas.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public List<ClientDto> getClientsForUser(User user) {
        return clientRepository.findByOwner(user).stream()
                .map(this::toDTO)
                .toList();
    }

    public Client getClientByIdAndOwner(Long id, User user) throws ForbiddenException {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found"));

        if (!client.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Unauthorized");
        }

        return client;
    }

    public Client createClient(Client client, User owner) throws ForbiddenException {
        boolean isFree = !owner.isSubscriptionActive();

        if (isFree) {
            long count = clientRepository.countByOwner(owner);
            if (count >= 1) {
                throw new ForbiddenException("Limite atteinte pour un compte gratuit.");
            }
        }

        client.setOwner(owner);
        return clientRepository.save(client);
    }

    public Client updateClient(Client existingClient, Client updatedData) {
        existingClient.setName(updatedData.getName());
        existingClient.setEmail(updatedData.getEmail());
        return clientRepository.save(existingClient);
    }

    public void deleteClient(Long id, User user) throws ForbiddenException {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found"));
        if (!client.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Unauthorized");
        }
        clientRepository.delete(client);
    }

    public ClientDto toDTO(Client client) {
        return ClientDto.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .build();
    }

    public Client fromDTO(ClientDto dto) {
        Client client = new Client();
        client.setId(dto.getId());
        client.setName(dto.getName());
        return client;
    }
}
