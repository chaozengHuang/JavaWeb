import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码哈希生成工具
 * 用法: 在 IDE 中直接运行 main 方法，修改 PASSWORD 变量为目标密码即可
 */
public class GenHash {
    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "123456";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        System.out.println("明文密码: " + password);
        System.out.println("BCrypt哈希: " + hash);
    }
}
