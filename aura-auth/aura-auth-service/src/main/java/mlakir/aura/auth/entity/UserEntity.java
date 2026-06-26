package mlakir.aura.auth.entity;

import java.util.*;

import jakarta.persistence.*;
import lombok.*;

import static lombok.AccessLevel.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @ManyToMany
    @JoinTable(name = "user_roles",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
        inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Set<RoleEntity> roles =  new HashSet<>();

}
