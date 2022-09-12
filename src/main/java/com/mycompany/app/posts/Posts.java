package com.mycompany.app.posts;

import com.mycompany.app.posts.cloudfunctions.CloudFunction;
import com.mycompany.app.posts.Storage;
import com.hashicorp.cdktf.Resource;
import com.hashicorp.cdktf.providers.google_beta.GoogleComputeGlobalAddress;
import com.hashicorp.cdktf.providers.google_beta.GoogleComputeGlobalAddressConfig;
import com.hashicorp.cdktf.providers.google_beta.GoogleComputeNetwork;
import com.hashicorp.cdktf.providers.google_beta.GoogleComputeNetworkConfig;
import com.hashicorp.cdktf.providers.google_beta.GoogleServiceNetworkingConnection;
import com.hashicorp.cdktf.providers.google_beta.GoogleServiceNetworkingConnectionConfig;
import software.constructs.Construct;

import java.util.List;


public class Posts extends Resource {

    private String httpsTriggerUrl;

    public Posts(Construct scope, String id, String environment, String user, String project, String dbPass){
        super(scope, id);

        GoogleComputeNetwork vpc = new GoogleComputeNetwork(this, "vpc-" + environment, GoogleComputeNetworkConfig.builder()
                .name("vpc-" + environment)
                .project(project)
                .autoCreateSubnetworks(false)
                .build()
        );

        GoogleComputeGlobalAddress privateIp = new GoogleComputeGlobalAddress(this, "internal-ip-address-" + environment + "-" + user, GoogleComputeGlobalAddressConfig.builder()
                .name("internal-ip-address-" + environment + "-" + user)
                .project(project)
                .purpose("VPC_PEERING")
                .addressType("INTERNAL")
                .prefixLength(16)
                .network(vpc.getId())
                .build()
        );

        GoogleServiceNetworkingConnection privateVpcConnection = new GoogleServiceNetworkingConnection(this, "vpc-connection-" + environment + "-" + user, GoogleServiceNetworkingConnectionConfig.builder()
                .network(vpc.getId())
                .service("servicenetworking.googleapis.com")
                .reservedPeeringRanges(List.of(privateIp.getName()))
                .build()
        );

        Storage storage = new Storage(this, "cloud-sql-" + environment + "-" + user,
                environment,
                user,
                project,
                privateVpcConnection,
                vpc.getId(),
                dbPass
        );

        CloudFunction cloudFunction = new CloudFunction(this, "cloud-function-" + environment + "-" + user,
                environment,
                user,
                project,
                vpc.getId(),
                storage.getDbHost(),
                storage.getDbName(),
                storage.getDbUserName(),
                storage.getDbUserPassword()
        );

        this.httpsTriggerUrl = cloudFunction.getHttpsTriggerUrl();
    }

    public String getHttpsTriggerUrl(){
        return this.httpsTriggerUrl;
    }
}
