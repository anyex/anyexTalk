#include <cstdio>
#include <sys/epoll.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <string.h>
#include <string>
#include <map>
#include<iostream>
#include<json/json.hpp>
#include <errno.h>
#include <fcntl.h>

#define MAX_EVENTS 10
#define PORT 9999
#define LISTEN_Q 5
#define BUF_SIZE 512
#define MAX_EVENTS 500

enum CMD_TYPE
{
LOGIN,
TALK,
RECONNECT,
ONLINELIST
};

struct UserName {
    int fd;
    std::string userName;
    std::string passWord;
};

using NJSON = nlohmann::json;
static std::map<std::string, UserName> userName;
std::map<int, std::string> eventBody;
void setnonblockingmode(int fd) {
    int flag = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flag | O_NONBLOCK);
}



int setup_listen_sock() {
    int sockfd, len;
    struct sockaddr_in address;

    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd == -1) {
        perror("socket:");
        exit(EXIT_FAILURE);
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    len = sizeof(address);

    if (bind(sockfd, (sockaddr*)&address, len) == -1) {
        perror("bind");
        exit(EXIT_FAILURE);
    }
    printf("Bind");
    int on = 1;
    if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) == -1) {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }
    printf("setsockopt");

    if (listen(sockfd, LISTEN_Q) == -1) {
        perror("listen");
        exit(EXIT_FAILURE);
    }
    printf("listen");

    return sockfd;

}




void Login(int fd, const NJSON& v) {
    std::string loginResponse;
    NJSON response;
    if (userName.find(v["ACCOUNT"].get<std::string>()) == userName.end()) {
        userName.insert({ v["ACCOUNT"].get<std::string>(), {fd, v["ACCOUNT"].get<std::string>() ,v["PASSWD"].get<std::string>()} });

    }
    {
        response["RESULT"] = "OK";
        response["DETAIL"] = "登录成功";
        response["LOGIN_STATE"] = 1;
        std::vector<NJSON> friends;
        for (auto it : userName) {
            NJSON friend_;
            friend_["ACCOUNT"] = it.second.userName;
            friends.push_back(friend_);

        }
        response["FRIENDS"] = friends;
    }
    loginResponse = response.dump();
    for (auto it:userName)
    {
        if (write(it.second.fd, loginResponse.c_str(), loginResponse.length()) > 0) {
            std::cout << "发送登录请求成功  fd= "<< it.second.fd << loginResponse << std::endl;
        }
        else {
            std::cout << "发送登录请求失败  " << loginResponse << std::endl;
        };
    }
   
}

void Talk(int fd, const NJSON& v) {
    NJSON response;
    std::string toResponse;
    auto me = userName[v["ME"].get<std::string>()];
    if (userName.find(v["TO"].get<std::string>()) != userName.end()) {
        response["TALK_STATE"] = true;
        auto to = userName[v["TO"].get<std::string>()];
      
        response["FROM"] = me.userName;
        response["TO"] = to.userName;
        response["MESSAGE"] = v["MESSAGE"].get<std::string>();
        response["TIME"] = v["TIME"].get<std::string>();
        toResponse = response.dump();
        if ((write(to.fd, toResponse.c_str(), toResponse.length()) )>0 ){
            std::cout << "FD="<<to.fd<<"发送消息成功  " << toResponse << std::endl;
        }
        //write(user.fd,)
    }
    else {
        response["TALK_STATE"] = false;

        response["ERROR"] = v["TO"].get<std::string>() + "离线";
        toResponse = response.dump();
        if ((write(me.fd, toResponse.c_str(), toResponse.length())) > 0) {
            std::cout << "发送消息成功  " << toResponse << std::endl;
        }
    }
}
void Reconnect(int fd, const NJSON& v) {

}
void OnlineList(int fd, const NJSON& v) {
    std::string loginResponse;
    NJSON response;
    response["RESULT"] = "OK";
    response["DETAIL"] = "登录成功";
    response["LOGIN_STATE"] = 1;
    std::vector<NJSON> friends;
    for (auto it : userName) {
        NJSON friend_;
        friend_["ACCOUNT"] = it.second.userName;
        friends.push_back(friend_);

    }
    response["FRIENDS"] = friends;

    if (write(fd, loginResponse.c_str(), loginResponse.length()) > 0) {
        std::cout << "发送好友列表请求成功  fd= " <<fd<< loginResponse << std::endl;
    }
    else {
        std::cout << "发送好友列表请求失败  " << loginResponse << std::endl;
    };
}
bool ProcessCmd(int fd, std::string& request) {
    //Json::Reader reader;
   // Json::Value value;
    if (NJSON::accept(request)) {
        auto js = NJSON::parse(request);
        if (!js.is_null()) {
            std::cout << "解析成功" << std::endl;
            switch (js["TYPE"].get<int>())
            {
            case LOGIN:
                Login(fd, js);
                break;
            case TALK:
                Talk(fd, js);
                break;
            case ONLINELIST:
                OnlineList(fd, js);

            }
        }
    }
    else {
        std::cout << request<<" 不是标准json字符串" << std::endl;
        return false;
    }
    return true;

}
void AddUserName(std::string& request) {

}



int main()
{
    int listen_sockfd, client_sockfd = 0, i = 0, ret = 0;
    socklen_t client_len;
    sockaddr_in client_address;
    char rwbuf[BUF_SIZE] = { 0 };

    listen_sockfd = setup_listen_sock();

    epoll_event eventArray[MAX_EVENTS];
    epoll_event event_sock;

    int epollfd = epoll_create(MAX_EVENTS);
    event_sock.events = EPOLLIN;
    event_sock.data.fd = listen_sockfd;

 /*   event_stdin.events = EPOLLIN;
    event_stdin.data.fd = STDIN_FILENO;*/

    if (epoll_ctl(epollfd, EPOLL_CTL_ADD, listen_sockfd, &event_sock) == -1) {
        perror("epool add failed");
        exit(EXIT_FAILURE);
    }

    //if (epoll_ctl(epollfd, EPOLL_CTL_ADD, STDIN_FILENO, &event_stdin) < 0) {
    //    perror("epool add STDIN_FILENO failed");
    //    exit(EXIT_FAILURE);
    //}

    while (true)
    {
        printf("server waiting\n");

        ret = epoll_wait(epollfd, eventArray, MAX_EVENTS, -1);
        printf("server GET \n");

        if (ret < 0) {
            perror("epoll_wait");
            exit(EXIT_FAILURE);
        }

        for (i = 0; i < ret; i++) {
            if ((eventArray[i].events & EPOLLERR) ||
                (eventArray[i].events & EPOLLHUP) ||
                !(eventArray[i].events & EPOLLIN))
            {
                perror("epoll error");
                epoll_ctl(epollfd, EPOLL_CTL_DEL, eventArray[i].data.fd, &eventArray[i]);
                eventBody.erase(eventArray[i].data.fd);
                close(eventArray[i].data.fd);
                printf("remove client on fd %d\n", client_sockfd);

            }
            else if (eventArray[i].data.fd == listen_sockfd) {
                client_len = sizeof(client_address);
                client_sockfd = accept(listen_sockfd, (struct sockaddr *)&client_address,
                    &client_len);
                setnonblockingmode(client_sockfd);
                struct epoll_event event_cli;
                event_cli.data.fd = client_sockfd;
                event_cli.events = EPOLLIN|EPOLLET;
                epoll_ctl(epollfd, EPOLL_CTL_ADD, client_sockfd,&event_cli);

                printf("adding client on fd %d\n", client_sockfd);
            }
            else {
                int  nread = 0;
                std::string cmd;
              //  ioctl(eventArray[i].data.fd, FIONREAD, &nread);
               while (true)
               {
                   nread = read(eventArray[i].data.fd, rwbuf, BUF_SIZE);
                   cmd.append(rwbuf);
                   printf("recv fd %d:%s,nread=%d\n", eventArray[i].data.fd, rwbuf, nread);
                   memset(rwbuf,'\0', sizeof(rwbuf));
                   if (nread == 0) {
                       close(eventArray[i].data.fd);
                       epoll_ctl(epollfd, EPOLL_CTL_DEL, eventArray[i].data.fd, &eventArray[i]);
                       eventBody.erase(eventArray[i].data.fd);
                       printf("removing client on fd:%d", eventArray[i].data.fd);
                       break;
                   }
                   else if (nread == -1) {
                       if (errno == EAGAIN) {
                           if (eventBody.find(eventArray[i].data.fd) == eventBody.end()) {
                               eventBody[eventArray[i].data.fd] = cmd;
                           }
                           else {
                               eventBody[eventArray[i].data.fd] = eventBody[eventArray[i].data.fd] + cmd;
                           }
                           cmd = eventBody[eventArray[i].data.fd];
                           if (ProcessCmd(eventArray[i].data.fd, cmd)) {
                               eventBody.erase(eventArray[i].data.fd);
                           }
                         
                           break;
                       }
                   }
               }

               //if (nread == 0) {
               //     close(eventArray[i].data.fd);
               //     epoll_ctl(epollfd, EPOLL_CTL_DEL, eventArray[i].data.fd, &eventArray[i]);
               //     close(eventArray[i].data.fd);
               //     printf("removing client on fd:%d", eventArray[i].data.fd);
               // }
               // else {
               //    std::string cmd(rwbuf);
               //   
               //    while (read(eventArray[i].data.fd, rwbuf, BUF_SIZE)>0)
               //    {
               //        cmd.append(rwbuf);
               //        memset(rwbuf, 0, BUFSIZ);
               //    }
               //     printf("recv fd %d:%s\n", eventArray[i].data.fd, rwbuf);
               //   
               //     ProcessCmd(eventArray[i].data.fd, cmd);
               //    
               // }
            }
          
        }
    }

    close(listen_sockfd);
    close(epollfd);
   

    return 0;
}