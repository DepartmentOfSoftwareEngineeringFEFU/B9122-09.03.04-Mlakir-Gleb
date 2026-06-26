package mlakir.aura.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import static lombok.AccessLevel.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "credentials")
public class CredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false)
    private String hash;

}
