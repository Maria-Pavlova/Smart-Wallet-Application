package app.web.dto;

import app.user.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
public class UserEditRequest {

    @Size(max = 25,message = "First name length must be no more than 24 symbols")
    private String firstName;
    @Size(max = 25, message = "Last name length must be no more than 24 symbols")
    private String lastName;
    @Email(message = "Email must be valid")
    private String email;
    @URL(message = "URL must be valid")
    private String profilePicture;


}
