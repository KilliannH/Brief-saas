package com.killiann.briefsaas.controller;

import com.killiann.briefsaas.dto.ClientDto;
import com.killiann.briefsaas.entity.Client;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.ForbiddenException;
import com.killiann.briefsaas.exception.NotFoundException;
import com.killiann.briefsaas.repository.ClientRepository;
import com.killiann.briefsaas.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientDto>> getClients(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(clientService.getClientsForUser(user));
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@AuthenticationPrincipal User user,
                                               @RequestBody Client client) throws ForbiddenException {
        return ResponseEntity.ok(clientService.createClient(client, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateClient(@PathVariable Long id,
                                                  @RequestBody ClientDto dto,
                                                  @AuthenticationPrincipal User user) throws ForbiddenException {
        Client client = clientService.getClientByIdAndOwner(id, user);

        Client updated = clientService.updateClient(client, clientService.fromDTO(dto));
        return ResponseEntity.ok(clientService.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id,
                                             @AuthenticationPrincipal User user) throws ForbiddenException {
        clientService.deleteClient(id, user);
        return ResponseEntity.noContent().build();
    }
}
