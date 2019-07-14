package org.micro.neural.idempotent;

import lombok.*;
import org.micro.neural.config.GlobalConfig;
import org.micro.neural.config.RuleConfig;

/**
 * The Idempotent Config.
 *
 * @author lry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IdempotentConfig extends RuleConfig {

    private static final long serialVersionUID = 4076904823256002967L;

    /**
     * The model of limiter
     */
    private GlobalConfig.Model model = GlobalConfig.Model.STAND_ALONE;

    // === concurrent limiter


}
