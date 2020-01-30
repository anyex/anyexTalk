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

#define MAX_EVENTS 10
#define PORT 9999
#define LISTEN_Q 5
#define BUF_SIZE 256
#define MAX_EVENTS 500

enum CMD_TYPE
{
    LOGIN,
    TALK
};

struct UserName {
    int fd;
    std::string userName;
    std::string passWord;
};

using NJSON = nlohmann::json;
static std::map<std::string, UserName> userName;



int setup_listen_sock() {
    int sockfd, len;
    struct sockaddr_in address;

    sockfd = socket(AF_INET,SOCK_STREAM,0);

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



void Login(int fd, const NJSON &v) {
    std::string loginResponse;
    NJSON response;
    if (userName.find(v["ACCOUNT"].get<std::string>()) == userName.end()) {
        userName.insert({ v["ACCOUNT"].get<std::string>(), {fd, v["ACCOUNT"].get<std::string>() ,v["PASSWD"].get<std::string>()} });
        response["RESULT"] = "OK";
        response["DETAIL"] = "登录成功";
        std::vector<NJSON> friends;
        for (auto it : userName) {
            NJSON friend_;
            friend_["ACCOUNT"] = it.second.userName;
            friends.push_back(friend_);
           
        }
        response["FRIENDS"] = friends;
    }
    else {
        response["RESULT"] = "FAILED";
        response["DETAIL"] = "登录失败";
    }
    loginResponse = response.dump();
    if (write(fd, loginResponse.c_str(), loginResponse.length()) > 0) {
        std::cout << "发送登录请求成功  " << loginResponse << std::endl;
    };
}

void Talk(int fd, const NJSON& v) {

}
void ProcessCmd(int fd, std::string& request) {
    //Json::Reader reader;
   // Json::Value value;
   auto js = NJSON::parse(request);
    if (!js.is_null()) {
        std::cout << "解析成功" << std::endl;
        switch (js["TYPE"].get<int>())
        {
        case LOGIN:
            Login(fd, js);
        default:
            Talk(fd, js);
            break;
        }
    }

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
    epoll_event event_stdin;

    int epollfd = epoll_create(MAX_EVENTS);
    event_sock.events = EPOLLIN;
    event_sock.data.fd = listen_sockfd;

    event_stdin.events = EPOLLIN;
    event_stdin.data.fd = STDIN_FILENO;

    if (epoll_ctl(epollfd, EPOLL_CTL_ADD, listen_sockfd, &event_sock) == -1) {
        perror("epool add failed");
        exit(EXIT_FAILURE);
    }

    if (epoll_ctl(epollfd, EPOLL_CTL_ADD, STDIN_FILENO, &event_stdin) < 0) {
        perror("epool add STDIN_FILENO failed");
        exit(EXIT_FAILURE);
    }

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
                close(eventArray[i].data.fd);
                exit(-1);
            }
            else if (eventArray[i].data.fd == listen_sockfd) {
                client_len = sizeof(client_address);
                client_sockfd = accept(listen_sockfd, (struct sockaddr *)&client_address,
                    &client_len);

                struct epoll_event event_cli;
                event_cli.data.fd = client_sockfd;
                event_cli.events = EPOLLIN;
                epoll_ctl(epollfd, EPOLL_CTL_ADD, client_sockfd,&event_cli);

                printf("adding client on fd %d\n", client_sockfd);
            }
            else {
                int  nread = 0;
                ioctl(eventArray[i].data.fd, FIONREAD, &nread);

                if (nread == 0) {
                    close(eventArray[i].data.fd);
                    epoll_ctl(epollfd, EPOLL_CTL_DEL, eventArray[i].data.fd, &eventArray[i]);

                    printf("removing client on fd:%d", eventArray[i].data.fd);
                }
                else {
                    read(eventArray[i].data.fd, rwbuf, BUF_SIZE);
                    printf("recv fd %d:%s\n", eventArray[i].data.fd, rwbuf);
                    std::string cmd(rwbuf);
                    ProcessCmd(eventArray[i].data.fd, cmd);
                   
                }
            }
          
        }
    }

    close(listen_sockfd);
    close(epollfd);
   

    return 0;
}