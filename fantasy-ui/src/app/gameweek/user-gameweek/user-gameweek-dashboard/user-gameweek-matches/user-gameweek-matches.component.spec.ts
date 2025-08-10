import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekMatchesComponent } from './user-gameweek-matches.component';

describe('UserGameweekMatchesComponent', () => {
  let component: UserGameweekMatchesComponent;
  let fixture: ComponentFixture<UserGameweekMatchesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekMatchesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekMatchesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
