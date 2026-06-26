package mlakir.aura.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.*;

import static lombok.AccessLevel.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "roles")
public class RoleEntity implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Override
    public String getAuthority() {
        return code;
    }

}
