package csim.scu.onlinejudge.dao.domain.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrectValue implements Serializable {

    private int score;
    private String comment;
}
