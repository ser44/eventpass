package ar.com.eventpass.evento;

import org.mapstruct.Mapper;

import ar.com.eventpass.evento.dto.EventoDto;
import ar.com.eventpass.evento.dto.EventoResumenDto;

/** Mapper entidad -> DTO (patrón Mapper, implementación generada por MapStruct). */
@Mapper(componentModel = "spring")
public interface EventoMapper {

    EventoDto aDto(Evento evento);

    EventoResumenDto aResumen(Evento evento);
}
