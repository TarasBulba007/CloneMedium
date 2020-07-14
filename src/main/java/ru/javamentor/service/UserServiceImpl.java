package ru.javamentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.javamentor.dao.NotificationDao;
import ru.javamentor.dao.UserDAO;
import ru.javamentor.model.Notification;
import ru.javamentor.model.User;

import java.util.*;

/**
 * Реализация интерфейса UserService
 *
 * @version 1.0
 * @author Java Mentor
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private UserDAO userDAO;
    private MailSender mailSender;
    private NotificationDao notificationDao;
    private final PasswordEncoder passwordEncoder;

    @Value("${site.link}")
    private String link;

    @Autowired
    public UserServiceImpl(UserDAO userDAO, MailSender mailSender, NotificationDao notificationDao, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.mailSender = mailSender;
        this.notificationDao = notificationDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * метод для добавления пользователя
     *
     * @param user - пользователь которого необходимо добавить
     * @return boolean удалось добавить или нет
     */
    @Transactional
    @Override
    public boolean addUser(User user) {
        user.setActivated(false);
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userDAO.addUser(user);

        if (!StringUtils.isEmpty(user.getUsername())) {
            sendCode(user);
        }
        return true;
    }

    /**
     * метод для добавления пользователя из социальной сети
     *
     * @param user - пользователь которого необходимо добавить
     * @return boolean удалось добавить или нет
     */
    @Transactional
    @Override
    public boolean addUserThroughSocialNetworks(User user) {
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        userDAO.addUser(user);
        return true;
    }

    /**
     * метод для отправки письма подтверждения на почту
     *
     * @param user - пользователь которому нужно отправить письмо
     */
    @Transactional
    @Override
    public void sendCode(User user) {
        String message = String.format(
                "Hello, %s \n" +
                        "Welcome to CloneMedium. Please visit next link for confirm email: <a target=\"_blank\" href=" + "%s" +
                        "registration/activate/%s" + ">Confirm</a>",
                user.getUsername(),
                link,
                user.getActivationCode()
        );
        mailSender.send(user.getUsername(), "Activation code", message);
    }

    /**
     * метод для отправки получения пользователя по его активационному коду
     *
     * @param code - строковое представление активационного кода
     * @return User - пользователь
     */
    @Transactional
    @Override
    public User findByActivationCode(String code) {
        return userDAO.findByActivationCode(code);
    }

    /**
     * метод для получения пользователя по уникальному id
     *
     * @param id - уникальный id пользователя
     * @return User - пользователь
     */
    @Transactional
    @Override
    public User getUserById(Long id) {
        return userDAO.getUserById(id);
    }

    @Transactional
    @Override
    public List<User> getAllUsers() {
        List<User> result = userDAO.getAllUsers();
        log.info("IN getAllUsers - {} users found", result.size());
        return result;
    }

    /**
     * метод для получения всего списка пользователей
     *
     * @return List пользоватей
     */
    @Transactional
    @Override
    public boolean updateUser(User user) {
        boolean isBCryptPassword = user.getPassword().length() < 45;
        if (isBCryptPassword) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userDAO.updateUser(user);
        return true;
    }

    /**
     * метод для удаления пользователя
     *
     * @param id - уникальный id пользователя в системе
     */
    @Transactional
    @Override
    public void removeUser(Long id) {
        userDAO.removeUser(id);
    }

    /**
     * метод для активации пользователя в системе
     *
     * @param code - активационный код пользователя
     * @return boolean - удалось активировать пользователя или нет
     */
    @Transactional
    @Override
    public boolean activateUser(String code) {
        User user = userDAO.findByActivationCode(code);

        if (user == null) {
            return false;
        }

        user.setActivationCode(null);
        user.setActivated(true);
        userDAO.updateUser(user);

        return true;
    }

    /**
     * метод для получения пользователя по его электронной почте
     *
     * @param email -  электронная почта пользователя
     * @return User пользователь
     */
    @Override
    public User getUserByEmail(String email) {
        return userDAO.getUserByUsername(email);
    }

    /**
     * метод для получения пользователя по его username
     *
     * @param username - имя пользователя
     * @return User пользователь
     */
    @Override
    public User getUserByUsername(String username) {
        return userDAO.getUserByUsername(username);
    }
    /**
     * метод для входа пользователя в систему by Spring Security
     *
     * @param username - логие
     * @param password - пароль
     * @param authorities - роли by Spring Security
     */
    @Override
    public void login(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password, authorities);
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authReq);
    }

    /**
     * Метод получения списка всех имен авторов не связанных с пользователем (ников)
     * @param username - имя пользователя
     * @return - список имен авторов (ников)
     */
    @Override
    public List<String> getAllSubscribesNotOfUser(String username) {
        List<String> authors;
        try {
            authors =  userDAO.getAllSubscribesNotOfUser(username);
            log.info("IN getAllSubscribesNotOfUser - {} authors found", authors.size());
        } catch (Exception e) {
            return null;
        }
        return authors;
    }

    /**
     * Метод получения списка подписок пользователя
     * @param username - имя пользователя
     * @return - список подписок
     */
    @Override
    public List<String> getAllSubscribesOfUser(String username) {
        List<String> subcribes;
        try {
            subcribes =  userDAO.getAllSubscribesOfUser(username);
            log.info("IN getAllSubscribesOfUser - {} authors found", subcribes.size());
        } catch (Exception e) {
            return null;
        }
        return subcribes;
    }

    /**
     * Метод добавления подписок
     * @param authors - авторы
     * @param subscriber - подписчик
     */
    @Transactional
    @Override
    public boolean changeSubscribe(Set<String> authors, String subscriber) {
        try {
            userDAO.deleteSubscribesOfUser(subscriber);
            for (String author : authors) {
                userDAO.addSubscribe(author, subscriber);
                log.info("Subscribe with author: " + author + " and subscriber: " + subscriber + "successful");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Метод создания уведомлений для всех подписчиков автора
     * @param author - автор
     */
    @Transactional
    @Override
    public boolean notifyAllSubscribersOfAuthor(String author, String title, String text) {
        try {
            List<User> subscribers = userDAO.getAllSubscribersOfAuthor(author);
            for (User subscriber : subscribers) {
                notificationDao.addNotification(new Notification(title, text, subscriber));
                log.info("Notification for {} is added", subscriber.getUsername());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
