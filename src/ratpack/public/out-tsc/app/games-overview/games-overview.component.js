var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
import { Component } from '@angular/core';
import { RankingItemService } from '../services/ranking-item.service';
import { GameService } from '../services/game.service';
import { TournamentGameRoundService } from '../services/tournament-game-round.service';
import { SharedCommunicationService } from '../services/shared-communication.service';
var GamesOverviewComponent = (function () {
    function GamesOverviewComponent(rankingItemService, gameService, tournamentGameRoundService, sharedCommunicationService) {
        var _this = this;
        this.rankingItemService = rankingItemService;
        this.gameService = gameService;
        this.tournamentGameRoundService = tournamentGameRoundService;
        this.sharedCommunicationService = sharedCommunicationService;
        this.oneRoundAtATime = true;
        this.noGameGenerationAlerts = [];
        this.soundFire = new Audio("/assets/sounds/fire.wav");
        sharedCommunicationService.playerForStatisticsChanged$.subscribe(function (playerForStatistics) {
            _this.playerForStatistics = playerForStatistics;
        });
        sharedCommunicationService.selectedPlayersChanged$.subscribe(function (selectedPlayers) {
            _this.selectedPlayers = selectedPlayers;
        });
    }
    GamesOverviewComponent.prototype.addNoGameGenerationAlert = function (msg, type) {
        this.noGameGenerationAlerts.push({ msg: msg, type: type, closable: false });
    };
    GamesOverviewComponent.prototype.ngOnInit = function () {
    };
    GamesOverviewComponent.prototype.getNumberOfSelectedPlayers = function () {
        if (this.selectedPlayers == null) {
            return "0";
        }
        else {
            return this.selectedPlayers.length.toString();
        }
    };
    GamesOverviewComponent.prototype.getImageUrl = function (playerName) {
        if (playerName == null) {
            return "assets/img/Wildcard.jpg";
        }
        else {
            return "assets/img/" + playerName + ".jpg";
        }
    };
    GamesOverviewComponent.prototype.getRankingItemsForIntense = function () {
        var _this = this;
        var intenseFound = false;
        if (this.oneRoundAtATime) {
            this.intenseArray = new Array(this.tempTournamentGameRounds[0].tournamentGames.length);
            this.currentPointsForPlayer = new Array();
            this.currentPositionForPlayer = new Array();
            var firstTournamentGameRound = this.tempTournamentGameRounds[0];
            this.rankingItemService.getRankingItems('alltime').
                then(function (rankingItems) {
                _this.rankingItemsForIntense = rankingItems;
                var i = 0;
                for (var _i = 0, _a = firstTournamentGameRound.tournamentGames; _i < _a.length; _i++) {
                    var game = _a[_i];
                    var combinedPointsTeamRed = 0;
                    var combinedPointsTeamBlue = 0;
                    for (var _b = 0, _c = _this.rankingItemsForIntense; _b < _c.length; _b++) {
                        var rankingItem = _c[_b];
                        if (rankingItem.name == game.player_red_1 ||
                            rankingItem.name == game.player_red_2 ||
                            rankingItem.name == game.player_blue_1 ||
                            rankingItem.name == game.player_blue_2) {
                            _this.currentPointsForPlayer[rankingItem.name.toLocaleLowerCase()] = rankingItem.points;
                            _this.currentPositionForPlayer[rankingItem.name.toLocaleLowerCase()] = rankingItem.position;
                        }
                        if (rankingItem.name == game.player_red_1 || (rankingItem.name == game.player_red_2)) {
                            combinedPointsTeamRed = combinedPointsTeamRed + rankingItem.points;
                        }
                        if (rankingItem.name == game.player_blue_1 || (rankingItem.name == game.player_blue_2)) {
                            combinedPointsTeamBlue = combinedPointsTeamBlue + rankingItem.points;
                        }
                    }
                    if (combinedPointsTeamRed > combinedPointsTeamBlue + 5) {
                        _this.intenseArray[i] = "blue";
                        intenseFound = true;
                    }
                    else if (combinedPointsTeamRed + 5 < combinedPointsTeamBlue) {
                        _this.intenseArray[i] = "red";
                        intenseFound = true;
                    }
                    else {
                        _this.intenseArray[i] = "";
                    }
                    i++;
                }
                if (intenseFound) {
                    _this.soundFire.play();
                }
            })
                .catch(function (err) {
                console.log('Der var fejl omkring generering af kampe');
                _this.addNoGameGenerationAlert('Noget gik galt i forsøget på at generere kampe. Tjek venligst at der er forbindelse til serveren og prøv så igen. Fejlen var: \'' + err + '\'', 'danger');
                _this.tempTournamentGameRounds = null;
            });
        }
        this.tournamentGameRounds = this.tempTournamentGameRounds;
        this.tempTournamentGameRounds = null;
    };
    GamesOverviewComponent.prototype.indmeldResultat = function (roundIndex, player_red_1, player_red_2, player_blue_1, player_blue_2, table, resultat, updateTime, points_at_stake) {
        var _this = this;
        this.noGameGenerationAlerts = [];
        if (this.tournamentGameRounds[roundIndex].tournamentGames[+table - 1].id == 0) {
            alert('Du kan ikke indmelde samme kamp 2 gange!');
            return;
        }
        this.gameService.indmeldResultat(player_red_1, player_red_2, player_blue_1, player_blue_2, table, resultat, updateTime, points_at_stake)
            .then(function (strRes) {
            _this.tournamentGameRounds[roundIndex].tournamentGames[+table - 1].id = 0;
            _this.informAboutNewMatchReported(strRes);
        }).catch(function (err) {
            _this.addNoGameGenerationAlert('Noget gik galt i forsøget på at indmelde resultatet af kampen på dette bord. Tjek venligst at der er forbindelse til serveren og prøv så igen. Fejlen var: \'' + err + '\'', 'danger');
        });
    };
    GamesOverviewComponent.prototype.changePlayerForStatistics = function (playerForStatistics) {
        this.sharedCommunicationService.informAboutPlayerForStatisticsChanged(playerForStatistics);
    };
    GamesOverviewComponent.prototype.informAboutNewMatchReported = function (information) {
        this.sharedCommunicationService.informAboutNewMatchReported(information);
    };
    GamesOverviewComponent.prototype.getTournamentGameRounds = function () {
        var _this = this;
        this.noGameGenerationAlerts = [];
        this.tournamentGameRoundService.getTournamentGameRounds(3, this.selectedPlayers).then(function (tournamentGameRounds) {
            _this.tempTournamentGameRounds = tournamentGameRounds;
            var today = new Date();
            var dd = today.getDate();
            var mm = (today.getMonth() + 1);
            var yyyy = today.getFullYear();
            var hours = today.getHours();
            var newHOURS;
            newHOURS = hours;
            if (hours < 10) {
                newHOURS = '0' + hours;
            }
            var minutes = today.getMinutes();
            var newMINUTES;
            newMINUTES = minutes;
            if (minutes < 10) {
                newMINUTES = '0' + minutes;
            }
            var seconds = today.getSeconds();
            var newSECONDS;
            newSECONDS = seconds;
            if (seconds < 10) {
                newSECONDS = '0' + seconds;
            }
            var newToday;
            var newDD;
            newDD = dd;
            if (dd < 10) {
                newDD = '0' + dd;
            }
            var newMM;
            newMM = mm;
            if (mm < 10) {
                newMM = '0' + mm;
            }
            newToday = yyyy + '/' + newMM + '/' + newDD + " " + newHOURS + ":" + newMINUTES + ":" + newSECONDS + "." + today.getMilliseconds();
            for (var _i = 0, _a = _this.tempTournamentGameRounds; _i < _a.length; _i++) {
                var tournamentGameRound = _a[_i];
                for (var _b = 0, _c = tournamentGameRound.tournamentGames; _b < _c.length; _b++) {
                    var game = _c[_b];
                    game.lastUpdated = newToday;
                    console.log("game:  + game");
                }
            }
            _this.getRankingItemsForIntense();
        }).catch(function (err) {
            console.log('der var fejl omkring generering af kampe');
            _this.addNoGameGenerationAlert('Noget gik galt i forsøget på at generere kampe. Tjek venligst at der er forbindelse til serveren og prøv så igen. Fejlen var: \'' + err + '\'', 'danger');
        });
    };
    return GamesOverviewComponent;
}());
GamesOverviewComponent = __decorate([
    Component({
        selector: 'games-overview',
        templateUrl: './games-overview.component.html',
        styleUrls: ['./games-overview.component.css']
    }),
    __metadata("design:paramtypes", [RankingItemService,
        GameService,
        TournamentGameRoundService,
        SharedCommunicationService])
], GamesOverviewComponent);
export { GamesOverviewComponent };
//# sourceMappingURL=../../../../src/app/games-overview/games-overview.component.js.map