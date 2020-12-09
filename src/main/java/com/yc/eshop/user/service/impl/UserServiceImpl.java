package com.yc.eshop.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yc.eshop.common.entity.User;
import com.yc.eshop.common.response.ApiResponse;
import com.yc.eshop.common.response.ResponseCode;
import com.yc.eshop.common.service.RedisService;
import com.yc.eshop.common.util.ShiroUtil;
import com.yc.eshop.common.vo.UserTokenVO;
import com.yc.eshop.user.mapper.UserMapper;
import com.yc.eshop.user.service.UserService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;

/**
 * @author 余聪
 * @date 2020/10/16
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Value("${user-token-expire}")
    Integer userTokenExpire;

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisService redisService;

    @Override
    public ApiResponse<Void> register(User userDTO) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", userDTO.getAccount());
        User userEx = userMapper.selectOne(queryWrapper);
        if (!Objects.isNull(userEx)) {
            return ApiResponse.failure(ResponseCode.NOT_ACCEPTABLE, "此账号已经注册！");
        }

        String salt = ShiroUtil.createSalt();
        String encryptPwd = ShiroUtil.saltEncrypt(userDTO.getPassword(), salt);

        User user = User.builder()
                .account(userDTO.getAccount())
                .password(encryptPwd)
                .salt(salt)
                .nickname("游客_" + userDTO.getAccount().substring(0, 5))
                .sex("保密")
                .headPic("")
                .coin(100000)
                .build();
        userMapper.insertUser(user);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<User> getOne(Integer id) {
        User user = userMapper.selectById(id);
        return ApiResponse.ok(user);
    }

    @Override
    public ApiResponse<?> login(User user) throws InvocationTargetException, IllegalAccessException {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", user.getAccount());
        User userEx = userMapper.selectOne(queryWrapper);
        if (Objects.isNull(userEx)) {
            return ApiResponse.failure(ResponseCode.NOT_ACCEPTABLE, "账号未注册！");
        }
        String encryptPwd = ShiroUtil.saltEncrypt(user.getPassword(), userEx.getSalt());
        if (Objects.equals(encryptPwd, userEx.getPassword())) {
            String token = UUID.randomUUID().toString();
            redisService.setExpire(token, userEx.getUserId().toString(), userTokenExpire);
            UserTokenVO userTokenVO = new UserTokenVO();
            BeanUtils.copyProperties(userTokenVO, userEx);
            userTokenVO.setToken(token);
            return ApiResponse.ok(userTokenVO);
        } else {
            return ApiResponse.failure(ResponseCode.NOT_ACCEPTABLE, "密码错误！");
        }
    }

    @Override
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Boolean delete = redisService.delete(token);
        if (!delete) {
            return ApiResponse.failure(ResponseCode.NOT_ACCEPTABLE, "已退出登录！");
        }
        return ApiResponse.ok();
    }
}
