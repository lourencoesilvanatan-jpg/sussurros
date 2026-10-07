package com.sussurros.assombracao.diretor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.level.ServerLevel;

import com.sussurros.Sussurros;

/**
 * Fila de tarefas temporizadas do sistema de horror.
 *
 * Preserva a fila que antes ficava dentro do Diretor.
 */
public final class Agenda {
    private static final List<Tarefa> TAREFAS = new ArrayList<>();

    private record Tarefa(long tick, Runnable acao) {
    }

    private Agenda() {
    }

    public static void agendar(ServerLevel level, int atrasoTicks, Runnable acao) {
        TAREFAS.add(new Tarefa(level.getGameTime() + atrasoTicks, acao));
    }

    public static void tick(ServerLevel level) {
        if (TAREFAS.isEmpty()) {
            return;
        }

        long tick = level.getGameTime();
        List<Tarefa> prontas = new ArrayList<>();
        Iterator<Tarefa> it = TAREFAS.iterator();
        while (it.hasNext()) {
            Tarefa t = it.next();
            if (t.tick() <= tick) {
                prontas.add(t);
                it.remove();
            }
        }

        for (Tarefa t : prontas) {
            try {
                t.acao().run();
            } catch (Exception ex) {
                Sussurros.LOGGER.error("Erro numa tarefa agendada", ex);
            }
        }
    }

    public static void limpar() {
        TAREFAS.clear();
    }
}
