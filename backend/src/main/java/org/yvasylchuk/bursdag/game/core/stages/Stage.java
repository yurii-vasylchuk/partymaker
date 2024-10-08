package org.yvasylchuk.bursdag.game.core.stages;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hibernate.annotations.DiscriminatorFormula;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.game.core.Game;
import org.yvasylchuk.bursdag.game.core.GameContext;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Entity
@Table(name = "stages")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Setter(value = AccessLevel.PROTECTED)
@Getter
@DiscriminatorFormula("type")
public abstract class Stage<CTX> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "stage_order", nullable = false)
    private Integer order;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private Stage.Type type;

    @Column(name = "context")
    @Getter(AccessLevel.PROTECTED)
    private String rawContext;


    public abstract void act(BursdagPrincipal player, AsyncAction action);

    public abstract boolean isReadyToComplete();

    public abstract Map<Integer, Integer> calculateScores();

    public abstract StageReadiness calculateStageReadiness();


    public void initializeStage(GameContext gameCtx) {
    }

    public void finalizeStage(GameContext gameCtx) {
    }


    public abstract CTX loadContext();

    @SneakyThrows
    protected void updateContext(CTX settings) {
        this.rawContext = Game.MAPPER.writeValueAsString(settings);
    }

    protected <T> T runInContext(Function<CTX, T> operation) {
        CTX context = loadContext();
        T result = operation.apply(context);
        updateContext(context);
        return result;
    }

    protected void doInContext(Consumer<CTX> operation) {
        CTX context = loadContext();
        operation.accept(context);
        updateContext(context);
    }


    public enum Type {
        REGISTRATION,
        GUESS,
        CONTEST,
        TOURNAMENT,
    }

    public record StageReadiness(Integer total, Integer ready) {
    }
}
